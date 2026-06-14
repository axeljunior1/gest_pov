package com.erp.products.config;

import com.erp.products.service.DemoDatasetSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Charge le jeu de donnees demo (migration V13) apres les initializers systeme
 * (utilisateurs, unites, entrepot).
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DemoDatasetInitializer implements ApplicationRunner, Ordered {

    private final DemoDatasetSeeder demoDatasetSeeder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            DemoDatasetSeeder.DemoSeedResult result = demoDatasetSeeder.seed();
            if ("CREATED".equals(result.status())) {
                jdbcTemplate.update(
                        "UPDATE demo_dataset_meta SET seeded_at = now() WHERE id = 1 AND seeded_at IS NULL");
                log.info(
                        "Migration V13 — jeu demo charge : {} produits, {} clients, {} ventes POS (SKU ref. {})",
                        result.products(), result.customers(), result.sales(), result.markerSku());
            }
        } catch (Exception e) {
            log.error("Echec du chargement du jeu de donnees demo (V13)", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
