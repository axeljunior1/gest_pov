package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.erp.products.settings.SettingKeys;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SettingsControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Set Cat"))))
                .andExpect(status().isCreated())
                .andReturn();
        Long categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asLong();

        String symbole = "u" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Piece", "symbole", symbole))))
                .andExpect(status().isCreated());

        MvcResult unitList = mockMvc.perform(get("/api/units")).andReturn();
        JsonNode units = objectMapper.readTree(unitList.getResponse().getContentAsString());
        Long unitId = units.get(units.size() - 1).get("id").asLong();

        String sku = "SET-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Settings",
                                "sku", sku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 10,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", sku + "-V1",
                                        "stock", 0,
                                        "prix", 10
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode product = objectMapper.readTree(productResult.getResponse().getContentAsString());
        productId = product.get("id").asLong();
        variantId = product.get("variantes").get(0).get("id").asLong();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "W" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot settings"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "A1",
                                "nom", "Allee 1"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void shouldListReferenceValues() throws Exception {
        mockMvc.perform(get("/api/settings/reference-values"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CURRENCY", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.CURRENCY[?(@.code=='EUR')].label", hasItem("Euro (EUR)")))
                .andExpect(jsonPath("$.LANGUAGE[?(@.code=='fr')].label", hasItem("Français")))
                .andExpect(jsonPath("$.POS_SALES_FLOW_MODE", hasSize(2)));
    }

    @Test
    void shouldRejectInvalidCurrency() throws Exception {
        mockMvc.perform(put("/api/settings/" + SettingKeys.APP_CURRENCY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "INVALID"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CURRENCY")));
    }

    @Test
    void shouldReadDefaultSettings() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$[?(@.key=='company.name')].value", hasItem("ERP Produits")));

        mockMvc.perform(get("/api/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("EUR")));

        mockMvc.perform(get("/api/settings/config/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowNegativeStock", is(false)))
                .andExpect(jsonPath("$.lowStockThresholdDefault").value(10));
    }

    @Test
    void shouldUpdateSettingWithPermission() throws Exception {
        mockMvc.perform(put("/api/settings/" + SettingKeys.COMPANY_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "Ma Societe ERP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", is("Ma Societe ERP")));

        mockMvc.perform(get("/api/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName", is("Ma Societe ERP")));
    }

    @Test
    void shouldDenyUpdateForViewer() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.VIEWER_EMAIL,
                TestAuthReferenceDataInitializer.VIEWER_PASSWORD);

        mockMvc.perform(put("/api/settings/" + SettingKeys.COMPANY_NAME)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "Hack"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldApplyCustomEntryPrefix() throws Exception {
        mockMvc.perform(put("/api/settings/" + SettingKeys.NUMBERING_ENTRY_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "ENT"))))
                .andExpect(status().isOk());

        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 5
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String entryNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("entryNumber").asText();
        org.junit.jupiter.api.Assertions.assertTrue(entryNumber.startsWith("ENT-"));
    }

    @Test
    void shouldApplyCustomExitPrefix() throws Exception {
        seedStock(10);

        mockMvc.perform(put("/api/settings/" + SettingKeys.NUMBERING_EXIT_PREFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "SOR"))))
                .andExpect(status().isOk());

        MvcResult createResult = mockMvc.perform(post("/api/stock/exits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "reason", "SALE",
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 1
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String exitNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("exitNumber").asText();
        org.junit.jupiter.api.Assertions.assertTrue(exitNumber.startsWith("SOR-"));
    }

    @Test
    void shouldBlockExcessiveIssueWhenNegativeStockDisabled() throws Exception {
        seedStock(5);

        mockMvc.perform(post("/api/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 10,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowExcessiveIssueWhenNegativeStockEnabled() throws Exception {
        seedStock(5);

        mockMvc.perform(put("/api/settings/" + SettingKeys.STOCK_ALLOW_NEGATIVE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "true"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 8,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldUseDefaultAlertThreshold() throws Exception {
        mockMvc.perform(put("/api/settings/" + SettingKeys.STOCK_LOW_THRESHOLD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "15"))))
                .andExpect(status().isOk());

        seedStock(12);

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private void seedStock(int quantity) throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", quantity,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }
}
