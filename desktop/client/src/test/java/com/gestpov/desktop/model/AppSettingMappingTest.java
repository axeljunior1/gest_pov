package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingMappingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsReferenceCategoryAndHelpers() throws Exception {
        AppSetting s = AppSetting.fromJson(mapper.readTree("""
                {"key":"app.currency","value":"EUR","description":"Devise","type":"STRING","referenceCategory":"CURRENCY"}
                """));
        assertEquals("app.currency", s.key());
        assertEquals("EUR", s.value());
        assertEquals("CURRENCY", s.referenceCategory());
        assertTrue(s.hasReferenceList());
        assertEquals("Devise", s.label());
    }

    @Test
    void booleanAndJsonFlags() throws Exception {
        AppSetting bool = AppSetting.fromJson(mapper.readTree(
                "{\"key\":\"stock.allow_negative\",\"value\":\"false\",\"type\":\"BOOLEAN\"}"));
        assertTrue(bool.isBoolean());
        AppSetting json = AppSetting.fromJson(mapper.readTree(
                "{\"key\":\"loyalty.tiers_config\",\"value\":\"[]\",\"type\":\"JSON\"}"));
        assertTrue(json.isJson());
    }
}
