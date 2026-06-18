package com.erp.products.service;

import com.erp.products.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataCleanupService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int cleanupDemoData() {
        int before = countDemoProducts();
        if (before == 0) {
            return 0;
        }
        log.warn("Suppression des donnees de demonstration ({} produits demo detectes)", before);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/cleanup-demo-selective.sql"));
        populator.setContinueOnError(false);
        try {
            populator.execute(dataSource);
        } catch (Exception e) {
            throw new BusinessException("Impossible de supprimer les donnees de demonstration : "
                    + e.getMessage());
        }
        log.info("Donnees de demonstration supprimees");
        return before;
    }

    public int countDemoProducts() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku LIKE 'DEMO-%'", Integer.class);
        return count != null ? count : 0;
    }

    public int countDemoCustomers() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM customers
                WHERE customer_number LIKE 'CUST-DEMO-%' OR LOWER(email) LIKE '%@demo.local'
                """, Integer.class);
        return count != null ? count : 0;
    }

    public int countDemoSuppliers() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM suppliers
                WHERE LOWER(email) LIKE '%@demo.local'
                   OR nom LIKE '% demo'
                   OR nom LIKE '%Demo SA%'
                   OR nom LIKE '%Demo SARL%'
                """, Integer.class);
        return count != null ? count : 0;
    }

    public int countDemoCategories() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE nom LIKE '% demo'", Integer.class);
        return count != null ? count : 0;
    }

    public int countDemoSales() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT s.id) FROM sales s
                JOIN sale_lines sl ON sl.sale_id = s.id
                JOIN products p ON p.id = sl.product_id
                WHERE p.sku LIKE 'DEMO-%'
                """, Integer.class);
        return count != null ? count : 0;
    }

    public int countDemoStockMovements() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM stock_movements
                WHERE reference_type = 'DEMO_SEED' OR utilisateur = 'demo-seed'
                """, Integer.class);
        return count != null ? count : 0;
    }
}
