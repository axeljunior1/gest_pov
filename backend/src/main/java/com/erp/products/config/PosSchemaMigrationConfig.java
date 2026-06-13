package com.erp.products.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
@Profile("!test")
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class PosSchemaMigrationConfig {

    @Bean
    public static BeanFactoryPostProcessor posSchemaMigrationDependencyConfigurer() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    beanFactory.getBeanDefinition("entityManagerFactory")
                            .setDependsOn("posSchemaMigrationRunner");
                }
            }
        };
    }

    @Bean
    public PosSchemaMigrationRunner posSchemaMigrationRunner(DataSource dataSource) {
        PosSchemaMigrationRunner runner = new PosSchemaMigrationRunner(dataSource);
        runner.run();
        return runner;
    }

    static final class PosSchemaMigrationRunner {

        private final DataSource dataSource;

        PosSchemaMigrationRunner(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        void run() {
            try (Connection conn = dataSource.getConnection()) {
                if (!isPostgres(conn) || !tableExists(conn, "pos_sessions")) {
                    return;
                }
                migrateSessionType(conn);
                migrateSalesSellerAndPaidAt(conn);
                migrateSalesStatusCheck(conn);
                migratePaymentCashierAndSession(conn);
                migrateProductPackagingColumns(conn);
                migrateSaleLinePackagingSnapshots(conn);
            } catch (SQLException e) {
                throw new IllegalStateException("Echec migration schema POS", e);
            }
        }

        private void migrateSessionType(Connection conn) throws SQLException {
            boolean hasColumn = columnExists(conn, "pos_sessions", "session_type");
            try (Statement st = conn.createStatement()) {
                if (!hasColumn) {
                    st.execute("""
                            ALTER TABLE pos_sessions
                            ADD COLUMN session_type varchar(20) NOT NULL DEFAULT 'CASHIER'
                            """);
                    log.info("Migration POS: colonne pos_sessions.session_type creee");
                    return;
                }
                int updated = st.executeUpdate(
                        "UPDATE pos_sessions SET session_type = 'CASHIER' WHERE session_type IS NULL");
                st.execute("ALTER TABLE pos_sessions ALTER COLUMN session_type SET DEFAULT 'CASHIER'");
                st.execute("ALTER TABLE pos_sessions ALTER COLUMN session_type SET NOT NULL");
                if (updated > 0) {
                    log.info("Migration POS: {} session(s) existante(s) initialisee(s) en CASHIER", updated);
                } else {
                    log.debug("Migration POS: pos_sessions.session_type deja conforme");
                }
            }
        }

        private void migrateSalesSellerAndPaidAt(Connection conn) throws SQLException {
            if (!tableExists(conn, "sales")) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "sales", "seller_id")) {
                    st.execute("ALTER TABLE sales ADD COLUMN seller_id bigint");
                    log.info("Migration POS: colonne sales.seller_id creee");
                }
                st.executeUpdate("""
                        UPDATE sales SET seller_id = cashier_id
                        WHERE seller_id IS NULL AND cashier_id IS NOT NULL
                        """);
                try {
                    st.execute("ALTER TABLE sales ALTER COLUMN seller_id SET NOT NULL");
                } catch (SQLException e) {
                    log.warn("Migration POS: seller_id contient encore des NULL — backfill requis");
                }
                if (!columnExists(conn, "sales", "paid_at")) {
                    st.execute("ALTER TABLE sales ADD COLUMN paid_at timestamp with time zone");
                    log.info("Migration POS: colonne sales.paid_at creee");
                }
                if (!columnExists(conn, "sales", "submitted_at")) {
                    st.execute("ALTER TABLE sales ADD COLUMN submitted_at timestamp with time zone");
                    log.info("Migration POS: colonne sales.submitted_at creee");
                }
                st.executeUpdate("""
                        UPDATE sales SET paid_at = validated_at
                        WHERE paid_at IS NULL AND validated_at IS NOT NULL
                        """);
            }
        }

        private void migrateSalesStatusCheck(Connection conn) throws SQLException {
            if (!tableExists(conn, "sales") || !constraintExists(conn, "sales", "sales_status_check")) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE sales DROP CONSTRAINT sales_status_check");
                st.execute("""
                        ALTER TABLE sales ADD CONSTRAINT sales_status_check CHECK (status IN (
                            'DRAFT',
                            'HOLD',
                            'PENDING_PAYMENT',
                            'PAID',
                            'VALIDATED',
                            'CANCELLED',
                            'REFUNDED',
                            'PARTIALLY_REFUNDED'
                        ))
                        """);
                log.info("Migration POS: contrainte sales_status_check mise a jour (PENDING_PAYMENT, PAID)");
            }
        }

        private static boolean constraintExists(Connection conn, String table, String constraint) throws SQLException {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("""
                         SELECT 1 FROM information_schema.table_constraints
                         WHERE table_schema = 'public'
                           AND table_name = '%s'
                           AND constraint_name = '%s'
                         """.formatted(table, constraint))) {
                return rs.next();
            }
        }

        private void migratePaymentCashierAndSession(Connection conn) throws SQLException {
            if (!tableExists(conn, "payments")) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "payments", "cashier_id")) {
                    st.execute("ALTER TABLE payments ADD COLUMN cashier_id bigint");
                    log.info("Migration POS: colonne payments.cashier_id creee");
                }
                if (!columnExists(conn, "payments", "pos_session_id")) {
                    st.execute("ALTER TABLE payments ADD COLUMN pos_session_id bigint");
                    log.info("Migration POS: colonne payments.pos_session_id creee");
                }
                st.executeUpdate("""
                        UPDATE payments p
                        SET cashier_id = s.cashier_id,
                            pos_session_id = COALESCE(s.payment_session_id, s.pos_session_id)
                        FROM sales s
                        WHERE p.sale_id = s.id
                          AND (p.cashier_id IS NULL OR p.pos_session_id IS NULL)
                        """);
                st.executeUpdate("""
                        DELETE FROM payments p
                        USING sales s
                        WHERE p.sale_id = s.id
                          AND s.status IN ('DRAFT', 'PENDING_PAYMENT', 'HOLD')
                        """);
            }
        }

        private void migrateProductPackagingColumns(Connection conn) throws SQLException {
            if (!tableExists(conn, "product_packagings") || !tableExists(conn, "products")) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                addColumnIfMissing(st, conn, "product_packagings", "prix_vente",
                        "ALTER TABLE product_packagings ADD COLUMN prix_vente numeric(19,4)");
                addColumnIfMissing(st, conn, "product_packagings", "prix_achat",
                        "ALTER TABLE product_packagings ADD COLUMN prix_achat numeric(19,4)");
                addColumnIfMissing(st, conn, "product_packagings", "default_vente",
                        "ALTER TABLE product_packagings ADD COLUMN default_vente boolean NOT NULL DEFAULT false");
                addColumnIfMissing(st, conn, "product_packagings", "default_achat",
                        "ALTER TABLE product_packagings ADD COLUMN default_achat boolean NOT NULL DEFAULT false");
                addColumnIfMissing(st, conn, "product_packagings", "actif",
                        "ALTER TABLE product_packagings ADD COLUMN actif boolean NOT NULL DEFAULT true");

                int priced = st.executeUpdate("""
                        UPDATE product_packagings pp
                        SET prix_vente = COALESCE(p.prix_vente, 0) * pp.quantite_base
                        FROM products p
                        WHERE pp.product_id = p.id
                          AND (pp.prix_vente IS NULL OR pp.prix_vente = 0)
                        """);
                st.executeUpdate("""
                        UPDATE product_packagings
                        SET default_achat = true
                        WHERE principal = true AND default_achat = false
                        """);
                st.executeUpdate("""
                        UPDATE product_packagings
                        SET actif = true
                        WHERE actif IS NULL
                        """);

                if (columnExists(conn, "product_packagings", "prix_vente")) {
                    st.execute("ALTER TABLE product_packagings ALTER COLUMN prix_vente SET NOT NULL");
                }
                if (priced > 0) {
                    log.info("Migration POS: {} conditionnement(s) — prix_vente initialise(s)", priced);
                }
                log.info("Migration POS: colonnes product_packagings (prix, actif, defaults) verifiees");
            }
        }

        private void migrateSaleLinePackagingSnapshots(Connection conn) throws SQLException {
            if (!tableExists(conn, "sale_lines")) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                addColumnIfMissing(st, conn, "sale_lines", "packaging_name_snapshot",
                        "ALTER TABLE sale_lines ADD COLUMN packaging_name_snapshot varchar(255)");
                addColumnIfMissing(st, conn, "sale_lines", "packaging_quantity_snapshot",
                        "ALTER TABLE sale_lines ADD COLUMN packaging_quantity_snapshot numeric(19,6)");
                addColumnIfMissing(st, conn, "sale_lines", "unit_price_snapshot",
                        "ALTER TABLE sale_lines ADD COLUMN unit_price_snapshot numeric(19,4)");

                st.executeUpdate("""
                        UPDATE sale_lines sl
                        SET packaging_name_snapshot = pp.nom,
                            packaging_quantity_snapshot = pp.quantite_base,
                            unit_price_snapshot = sl.unit_price
                        FROM product_packagings pp
                        WHERE sl.packaging_id = pp.id
                          AND sl.packaging_name_snapshot IS NULL
                        """);
                st.executeUpdate("""
                        UPDATE sale_lines
                        SET unit_price_snapshot = unit_price
                        WHERE unit_price_snapshot IS NULL
                        """);
                log.info("Migration POS: colonnes sale_lines snapshots verifiees");
            }
        }

        private void addColumnIfMissing(Statement st, Connection conn, String table, String column, String ddl)
                throws SQLException {
            if (!columnExists(conn, table, column)) {
                st.execute(ddl);
                log.info("Migration POS: colonne {}.{} creee", table, column);
            }
        }

        private static boolean isPostgres(Connection conn) throws SQLException {
            DatabaseMetaData meta = conn.getMetaData();
            String product = meta.getDatabaseProductName();
            return product != null && product.toLowerCase().contains("postgresql");
        }

        private static boolean tableExists(Connection conn, String table) throws SQLException {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getTables(null, "public", table, new String[]{"TABLE"})) {
                return rs.next();
            }
        }

        private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, table, column)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getColumns(null, "public", table, column)) {
                return rs.next();
            }
        }
    }
}
