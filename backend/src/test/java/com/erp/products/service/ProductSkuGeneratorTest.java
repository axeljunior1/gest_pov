package com.erp.products.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSkuGeneratorTest {

    @Test
    void shouldBuildProductSkuFromName() {
        assertEquals("CHAUSSURE-RUNNING", ProductSkuGenerator.baseFromProductName("Chaussure Running"));
    }

    @Test
    void shouldBuildVariantSuffixFromColorAndSize() {
        assertEquals("BK-M", ProductSkuGenerator.variantSuffix("Noir", "M", 1));
        assertEquals("RD-L", ProductSkuGenerator.variantSuffix("Rouge", "L", 1));
    }

    @Test
    void shouldFallbackToVariantIndexWhenNoAttributes() {
        assertEquals("V2", ProductSkuGenerator.variantSuffix(null, null, 2));
    }

    @Test
    void shouldEnsureUniqueWithNumericSuffix() {
        assertEquals("ABC-2", ProductSkuGenerator.ensureUnique("ABC", sku -> "ABC".equals(sku)));
    }
}
