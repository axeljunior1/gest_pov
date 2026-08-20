package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrandMappingTest {

    @Test
    void fromJson() throws Exception {
        var node = new ObjectMapper().readTree("{\"id\":7,\"nom\":\"Lacoste\",\"createdAt\":\"2026-01-01T00:00:00Z\"}");
        Brand brand = Brand.fromJson(node);
        assertEquals(7L, brand.id());
        assertEquals("Lacoste", brand.nom());
        assertEquals("2026-01-01T00:00:00Z", brand.createdAt());
        assertNull(brand.updatedAt());
    }
}
