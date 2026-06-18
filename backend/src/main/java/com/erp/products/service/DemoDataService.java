package com.erp.products.service;

import com.erp.products.config.SeedProperties;
import com.erp.products.dto.DemoDataActionResponse;
import com.erp.products.dto.DemoDataStatusResponse;
import com.erp.products.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DemoDataService {

    private final SeedProperties seedProperties;
    private final DemoDatasetSeeder demoDatasetSeeder;
    private final DemoDataCleanupService demoDataCleanupService;
    private final SettingsService settingsService;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataStatusResponse getStatus() {
        int products = demoDataCleanupService.countDemoProducts();
        boolean present = products > 0;
        return DemoDataStatusResponse.builder()
                .demoPresent(present)
                .demoAutoEnabled(seedProperties.isDemoAuto())
                .demoManualEnabled(seedProperties.isDemoEnabled())
                .demoProducts(products)
                .demoCategories(demoDataCleanupService.countDemoCategories())
                .demoCustomers(demoDataCleanupService.countDemoCustomers())
                .demoSuppliers(demoDataCleanupService.countDemoSuppliers())
                .demoSales(demoDataCleanupService.countDemoSales())
                .demoStockMovements(demoDataCleanupService.countDemoStockMovements())
                .markerSku(DemoDatasetSeeder.MARKER_SKU)
                .setupCompleted(settingsService.isSetupCompleted())
                .message(present
                        ? "Des donnees de demonstration sont presentes (prefixe SKU DEMO-)."
                        : "Aucune donnee de demonstration detectee.")
                .build();
    }

    @Transactional
    public DemoDataActionResponse generate(boolean force) {
        assertDemoEnabled();
        if (!force && demoDataCleanupService.countDemoProducts() > 0) {
            throw new BusinessException(
                    "Des donnees de demonstration existent deja. Supprimez-les d'abord ou confirmez le remplacement.");
        }
        if (force && demoDataCleanupService.countDemoProducts() > 0) {
            demoDataCleanupService.cleanupDemoData();
        }
        DemoDatasetSeeder.DemoSeedResult result = demoDatasetSeeder.seed();
        if ("ALREADY_EXISTS".equals(result.status())) {
            throw new BusinessException("Le jeu de demonstration est deja present.");
        }
        jdbcTemplate.update(
                "UPDATE demo_dataset_meta SET seeded_at = now() WHERE id = 1 AND seeded_at IS NULL");
        return DemoDataActionResponse.builder()
                .status(result.status())
                .message("Donnees de demonstration generees avec succes.")
                .products(result.products())
                .customers(result.customers())
                .sales(result.sales())
                .build();
    }

    @Transactional
    public DemoDataActionResponse cleanup() {
        assertDemoEnabled();
        int categories = demoDataCleanupService.countDemoCategories();
        int suppliers = demoDataCleanupService.countDemoSuppliers();
        int removed = demoDataCleanupService.cleanupDemoData();
        if (removed == 0) {
            return DemoDataActionResponse.builder()
                    .status("EMPTY")
                    .message("Aucune donnee de demonstration a supprimer.")
                    .build();
        }
        return DemoDataActionResponse.builder()
                .status("DELETED")
                .message("Donnees de demonstration supprimees. Les donnees reelles sont conservees.")
                .products(removed)
                .categoriesRemoved(categories)
                .suppliersRemoved(suppliers)
                .build();
    }

    private void assertDemoEnabled() {
        if (!seedProperties.isDemoEnabled()) {
            throw new BusinessException("La gestion des donnees de demonstration est desactivee sur ce serveur.");
        }
    }
}
