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
 * Charge le jeu de donnees demo uniquement si {@code app.seed.demo-auto=true}.
 * En production, la generation se fait manuellement via /api/admin/demo-data/generate.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DemoDatasetInitializer implements ApplicationRunner, Ordered {

    private final DemoDatasetSeeder demoDatasetSeeder;
    private final JdbcTemplate jdbcTemplate;
    private final SeedProperties seedProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.isDemoAuto()) {
            log.info("Seed demo automatique desactive (app.seed.demo-auto=false)");
            return;
        }
        if (!seedProperties.isDemoEnabled()) {
            log.info("Donnees demo desactivees (app.seed.demo-enabled=false)");
            return;
        }
        try {
            DemoDatasetSeeder.DemoSeedResult result = demoDatasetSeeder.seed();
            if ("CREATED".equals(result.status())) {
                jdbcTemplate.update(
                        "UPDATE demo_dataset_meta SET seeded_at = now() WHERE id = 1 AND seeded_at IS NULL");
                log.info(
                        "Jeu demo auto-charge : {} produits, {} clients, {} ventes (SKU ref. {})",
                        result.products(), result.customers(), result.sales(), result.markerSku());
            }
        } catch (Exception e) {
            log.error("Echec du chargement automatique du jeu demo", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
