package com.gestpov.desktop.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductValidatorTest {

    @Test
    void nameRequired() {
        assertEquals(ProductValidator.NAME_REQUIRED, ProductValidator.validateName(""));
        assertEquals(ProductValidator.NAME_REQUIRED, ProductValidator.validateName(null));
        assertNull(ProductValidator.validateName(" Cahier "));
    }

    @Test
    void optionalPrice() {
        assertNull(ProductValidator.validateOptionalPrice(""));
        assertNull(ProductValidator.validateOptionalPrice("3,50"));
        assertEquals(ProductValidator.PRICE_NEGATIVE, ProductValidator.validateOptionalPrice("-1"));
        assertEquals(ProductValidator.PRICE_INVALID, ProductValidator.validateOptionalPrice("abc"));
        assertEquals(new BigDecimal("3.50"), ProductValidator.parsePrice("3,50"));
    }

    @Test
    void requiredPrice() {
        assertEquals(ProductValidator.PRICE_REQUIRED, ProductValidator.validateRequiredPrice(""));
        assertNull(ProductValidator.validateRequiredPrice("10"));
    }
}
