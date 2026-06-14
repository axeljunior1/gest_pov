package com.erp.products.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @deprecated Utiliser {@link DemoDatasetSeeder}. Conserve pour compatibilite API dev.
 */
@Service
@RequiredArgsConstructor
public class DemoDataSeeder {

    private final DemoDatasetSeeder demoDatasetSeeder;

    public static final String DEMO_PRODUCT_SKU = DemoDatasetSeeder.PRIMARY_SKU;

    @Transactional
    public DemoSeedResult seed() {
        DemoDatasetSeeder.DemoSeedResult result = demoDatasetSeeder.seed();
        if ("CREATED".equals(result.status())) {
            return DemoSeedResult.created(null, result.markerSku());
        }
        return DemoSeedResult.alreadyExists(null, result.markerSku());
    }

    public record DemoSeedResult(String status, Long productId, String productSku) {
        static DemoSeedResult created(Long id, String sku) {
            return new DemoSeedResult("CREATED", id, sku);
        }

        static DemoSeedResult alreadyExists(Long id, String sku) {
            return new DemoSeedResult("ALREADY_EXISTS", id, sku);
        }
    }
}
