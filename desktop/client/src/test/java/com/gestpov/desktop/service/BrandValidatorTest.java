package com.gestpov.desktop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrandValidatorTest {

    @Test
    void emptyNameRejected() {
        assertEquals(BrandValidator.NAME_REQUIRED, BrandValidator.validateName(""));
        assertEquals(BrandValidator.NAME_REQUIRED, BrandValidator.validateName("   "));
        assertEquals(BrandValidator.NAME_REQUIRED, BrandValidator.validateName(null));
    }

    @Test
    void trimsName() {
        assertNull(BrandValidator.validateName("  Nike  "));
        assertEquals("Nike", BrandValidator.normalizeName("  Nike  "));
    }
}
