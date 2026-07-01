package com.erp.products.support;

import com.erp.products.config.ReferenceValueTestInitializer;
import com.erp.products.config.TestAlertReferenceDataInitializer;
import com.erp.products.config.TestAppSettingsInitializer;
import com.erp.products.config.TestAuthReferenceDataInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nettoie la base H2 entre tests d'intégration sans redémarrer le contexte Spring.
 */
@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDatabaseCleaner {

    private static final Set<String> EXCLUDED_TABLES = Set.of(
            "flyway_schema_history"
    );

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final ReferenceValueTestInitializer referenceValueTestInitializer;
    private final TestAuthReferenceDataInitializer authReferenceDataInitializer;
    private final TestAlertReferenceDataInitializer alertReferenceDataInitializer;
    private final TestAppSettingsInitializer testAppSettingsInitializer;

    public void resetDatabase() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            truncateApplicationTables();
            reseedReferenceData();
        });
    }

    private void truncateApplicationTables() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """,
                String.class);

        Set<String> seen = new HashSet<>();
        for (String table : tables) {
            if (table == null || !seen.add(table.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (EXCLUDED_TABLES.contains(table.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String quoted = quoteTable(table);
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + quoted + " RESTART IDENTITY");
            } catch (Exception truncateError) {
                log.debug("TRUNCATE {} impossible ({}), fallback DELETE", table, truncateError.getMessage());
                jdbcTemplate.execute("DELETE FROM " + quoted);
            }
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void reseedReferenceData() {
        referenceValueTestInitializer.seedAll();
        authReferenceDataInitializer.seedAll();
        alertReferenceDataInitializer.seedAll();
        testAppSettingsInitializer.seedAll();
    }

    private static String quoteTable(String table) {
        return "\"" + table.replace("\"", "\"\"") + "\"";
    }
}
