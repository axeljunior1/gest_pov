package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Alert Cat"))))
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

        String sku = "ALT-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Alertes",
                                "sku", sku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 10,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", java.util.List.of(Map.of(
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
                                "nom", "Entrepot alertes"
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
    void shouldTriggerLowStockAndOutOfStockAfterMovements() throws Exception {
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

        mockMvc.perform(get("/api/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 6,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("LOW_STOCK")))
                .andExpect(jsonPath("$[0].status", is("OPEN")));

        mockMvc.perform(post("/api/stock/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 9,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("OUT_OF_STOCK")));

        mockMvc.perform(get("/api/notifications").param("userId", "admin@erp.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldAcknowledgeAndResolveAlert() throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 5,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        MvcResult alerts = mockMvc.perform(get("/api/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        Long alertId = objectMapper.readTree(alerts.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(post("/api/alerts/" + alertId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("user", "Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACKNOWLEDGED")));

        mockMvc.perform(post("/api/alerts/" + alertId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("user", "Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));
    }

    @Test
    void shouldAutoResolveLowStockWhenRestocked() throws Exception {
        seedStock(15);
        issueStock(6);

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 20,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/alerts").param("status", "RESOLVED").param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resolvedAt", notNullValue()));
    }

    @Test
    void shouldNotDuplicateOpenAlertsOnRepeatedTriggers() throws Exception {
        seedStock(15);
        issueStock(6);
        issueStock(1);

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].triggerCount", greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldTriggerExpirySoonAfterEntryValidation() throws Exception {
        LocalDate soon = LocalDate.now().plusDays(7);
        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 10,
                                        "lotNumber", "LOT-EXP-SOON",
                                        "expiryDate", soon.toString()
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("user", "Test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "EXPIRY_SOON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lotNumero", is("LOT-EXP-SOON")))
                .andExpect(jsonPath("$[0].severity", is("WARNING")));
    }

    @Test
    void shouldTriggerExpiredAfterEntryValidation() throws Exception {
        LocalDate expired = LocalDate.now().minusDays(1);
        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 5,
                                        "lotNumber", "LOT-EXP-PAST",
                                        "expiryDate", expired.toString()
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("user", "Test"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/alerts").param("status", "OPEN").param("type", "EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].severity", is("CRITICAL")));
    }

    @Test
    void shouldFilterAlertsByProductAndReturnDetail() throws Exception {
        seedStock(5);

        MvcResult alerts = mockMvc.perform(get("/api/alerts")
                        .param("status", "OPEN")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        Long alertId = objectMapper.readTree(alerts.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(get("/api/alerts/" + alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(alertId.intValue())))
                .andExpect(jsonPath("$.productId", is(productId.intValue())))
                .andExpect(jsonPath("$.triggeredValue", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void shouldDenyAlertsReadForViewerWithoutPermission() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.VIEWER_EMAIL,
                TestAuthReferenceDataInitializer.VIEWER_PASSWORD);

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
