package com.gestpov.desktop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CategoryValidatorTest {

    @Test
    void emptyNameRejected() {
        assertEquals(CategoryValidator.NAME_REQUIRED, CategoryValidator.validateName(""));
        assertEquals(CategoryValidator.NAME_REQUIRED, CategoryValidator.validateName("   "));
        assertEquals(CategoryValidator.NAME_REQUIRED, CategoryValidator.validateName(null));
    }

    @Test
    void trimsName() {
        assertNull(CategoryValidator.validateName("  Sport  "));
        assertEquals("Sport", CategoryValidator.normalizeName("  Sport  "));
    }
}
