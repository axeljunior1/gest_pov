package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
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

class DashboardControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Dash Cat"))))
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

        String sku = "DSH-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Dashboard",
                                "sku", sku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixAchat", 5,
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
                                "nom", "Entrepot dashboard"
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
    void shouldReturnCorrectStockSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts", is(1)))
                .andExpect(jsonPath("$.totalStockQuantity").value(0))
                .andExpect(jsonPath("$.stockValue").value(0))
                .andExpect(jsonPath("$.outOfStockProducts", is(1)))
                .andExpect(jsonPath("$.lowStockProducts", is(0)));

        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 15,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts", is(1)))
                .andExpect(jsonPath("$.totalStockQuantity").value(15))
                .andExpect(jsonPath("$.stockValue").value(75))
                .andExpect(jsonPath("$.outOfStockProducts", is(0)))
                .andExpect(jsonPath("$.lowStockProducts", is(0)));
    }

    @Test
    void shouldReturnCorrectLowStockAndOutOfStockCounts() throws Exception {
        seedStock(15);
        issueStock(6);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockProducts", is(1)))
                .andExpect(jsonPath("$.outOfStockProducts", is(0)));

        issueStock(9);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outOfStockProducts", is(1)))
                .andExpect(jsonPath("$.lowStockProducts", is(0)));
    }

    @Test
    void shouldReturnCorrectOpenAlerts() throws Exception {
        seedStock(5);

        mockMvc.perform(get("/api/dashboard/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openAlerts", is(1)))
                .andExpect(jsonPath("$.openLowStock", is(1)))
                .andExpect(jsonPath("$.openOutOfStock", is(0)));
    }

    @Test
    void shouldReturnRecentMovements() throws Exception {
        seedStock(10);
        issueStock(3);

        mockMvc.perform(get("/api/dashboard/movements/recent").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].productId", is(productId.intValue())));
    }

    @Test
    void shouldReturnTopMovedProducts() throws Exception {
        seedStock(20);
        issueStock(5);
        issueStock(3);

        mockMvc.perform(get("/api/dashboard/products/top-moved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId", is(productId.intValue())))
                .andExpect(jsonPath("$[0].movementCount", greaterThanOrEqualTo(3)));
    }

    @Test
    void shouldReturnWarehouseSummary() throws Exception {
        seedStock(12);

        mockMvc.perform(get("/api/dashboard/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].warehouseId", is(warehouseId.intValue())))
                .andExpect(jsonPath("$[0].totalQuantity").value(12))
                .andExpect(jsonPath("$[0].stockValue").value(60));
    }

    @Test
    void shouldDenyDashboardForViewerWithoutPermission() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.VIEWER_EMAIL,
                TestAuthReferenceDataInitializer.VIEWER_PASSWORD);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotModifyStockWhenReadingDashboard() throws Exception {
        seedStock(10);

        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/alerts")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/movements/recent")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/products/top-moved")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/warehouses")).andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10));
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

    private void issueStock(int quantity) throws Exception {
        mockMvc.perform(post("/api/stock/issue")
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
