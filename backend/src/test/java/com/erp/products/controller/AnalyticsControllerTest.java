package com.erp.products.controller;

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

class AnalyticsControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long warehouseId;
    private Long locationId;
    private Long productId;
    private Long variantId;
    private String adminToken;
    private String managerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Analytics Cat"))))
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

        String sku = "AN-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Analytics",
                                "sku", sku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 50,
                                "prixAchat", 20,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", sku + "-V1",
                                        "stock", 0,
                                        "prix", 50,
                                        "codeBarre", "ANBAR" + UUID.randomUUID().toString().substring(0, 8)
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();
        variantId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("variantes").get(0).get("id").asLong();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "WA" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot Analytics"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "DEFAULT", "nom", "Zone A"))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(100);

        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        managerToken = loginToken("manager@erp.local", "Manager2026!");
        cashierToken = loginToken("cashier@erp.local", "Cashier2026!");
    }

    @Test
    void shouldReturnOverviewWithSalesMetrics() throws Exception {
        createValidatedSale(adminToken, 2);

        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueToday.current").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.salesCountToday.current").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.currency", notNullValue()));
    }

    @Test
    void shouldReturnTimelineAndTopProducts() throws Exception {
        createValidatedSale(adminToken, 3);

        mockMvc.perform(get("/api/analytics/sales/timeline")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY")
                        .param("granularity", "HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", not(empty())));

        mockMvc.perform(get("/api/analytics/products/top")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId", is(productId.intValue())))
                .andExpect(jsonPath("$.items[0].quantitySold").value(greaterThan(0.0)));
    }

    @Test
    void shouldReturnPaymentsCategoriesAndCashiers() throws Exception {
        createValidatedSale(adminToken, 1);

        mockMvc.perform(get("/api/analytics/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods", not(empty())));

        mockMvc.perform(get("/api/analytics/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));

        mockMvc.perform(get("/api/analytics/cashiers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    @Test
    void shouldReturnStockAndBusinessAlerts() throws Exception {
        mockMvc.perform(get("/api/analytics/stock-alerts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "THIS_MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/analytics/business-alerts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("period", "THIS_MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldExportProductsCsv() throws Exception {
        createValidatedSale(adminToken, 1);

        mockMvc.perform(get("/api/analytics/export")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("period", "TODAY")
                        .param("type", "products"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("analytics-products.csv")));
    }

    @Test
    void cashierShouldAccessOwnSalesOverview() throws Exception {
        createValidatedSale(cashierToken, 1);

        mockMvc.perform(get("/api/analytics/overview")
                        .header("Authorization", "Bearer " + cashierToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesCountToday.current", greaterThanOrEqualTo(1)));
    }

    @Test
    void cashierShouldNotExportAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/export")
                        .header("Authorization", "Bearer " + cashierToken)
                        .param("type", "products"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerShouldAccessFullAnalytics() throws Exception {
        createValidatedSale(managerToken, 1);

        mockMvc.perform(get("/api/analytics/cashiers")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("period", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    private void createValidatedSale(String token, int qty) throws Exception {
        mockMvc.perform(post("/api/pos/sessions/open")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());

        MvcResult saleResult = mockMvc.perform(post("/api/pos/sales")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/pos/sales/" + saleId + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", qty,
                                "unitPrice", 50))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pos/sales/" + saleId + "/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", qty * 50)),
                                "cashReceived", qty * 50))))
                .andExpect(status().isOk());
    }

    private void seedStock(int qty) throws Exception {
        MvcResult entry = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "reference", "AN-ENTRY-" + UUID.randomUUID().toString().substring(0, 6),
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", qty
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(entry.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate"))
                .andExpect(status().isOk());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
