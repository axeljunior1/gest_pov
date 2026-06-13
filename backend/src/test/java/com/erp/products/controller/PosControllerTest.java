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

class PosControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private String productSku;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "POS Cat"))))
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

        productSku = "POS-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit POS",
                                "sku", productSku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 25,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", productSku + "-V1",
                                        "stock", 0,
                                        "prix", 25,
                                        "codeBarre", "POSBAR" + UUID.randomUUID().toString().substring(0, 8)
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
                                "code", "W" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot POS"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone POS"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(20);
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
    }

    @Test
    void shouldOpenAndCloseSession() throws Exception {
        mockMvc.perform(post("/api/pos/sessions/open")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("OPEN")));

        mockMvc.perform(get("/api/pos/sessions/current")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId", is(warehouseId.intValue())));

        mockMvc.perform(post("/api/pos/sessions/close")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleCount", is(0)));
    }

    @Test
    void shouldValidateSaleAndDecrementStock() throws Exception {
        openSession();

        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0.0)));

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].quantityInput", is(3.0)));

        JsonNode saleBeforePay = objectMapper.readTree(
                mockMvc.perform(auth(get("/api/pos/sales/" + saleId))).andReturn().getResponse().getContentAsString());
        double totalPay = saleBeforePay.get("total").asDouble();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", totalPay)),
                                "cashReceived", totalPay + 25))))
                .andExpect(jsonPath("$.status", is("VALIDATED")));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(17.0));

        mockMvc.perform(get("/api/stock/movements").param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].movementType", is("OUT")));

        mockMvc.perform(auth(get("/api/pos/sales/" + saleId + "/ticket")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber", notNullValue()));
    }

    @Test
    void shouldSearchBySkuAndBarcode() throws Exception {
        openSession();
        mockMvc.perform(auth(get("/api/pos/catalog/search")).param("q", productSku).param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku", is(productSku)));
    }

    @Test
    void shouldHoldAndResumeSaleWithoutStockChange() throws Exception {
        openSession();
        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales"))).andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantityInput", 5))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/hold"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Client oublie portefeuille"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HOLD")));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(20.0));

        mockMvc.perform(auth(get("/api/pos/sales/hold")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/resume")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void shouldRejectInsufficientStock() throws Exception {
        openSession();
        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales"))).andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantityInput", 25))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", 9999))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDenySessionCloseForCashierWithoutPermission() throws Exception {
        openSessionAsCashier();
        String token = loginToken(
                TestAuthReferenceDataInitializer.CASHIER_EMAIL,
                TestAuthReferenceDataInitializer.CASHIER_PASSWORD);

        mockMvc.perform(post("/api/pos/sessions/close")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldProcessMixedPayment() throws Exception {
        openSession();
        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales"))).andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantityInput", 4))))
                .andExpect(status().isOk());

        JsonNode sale = objectMapper.readTree(mockMvc.perform(auth(get("/api/pos/sales/" + saleId))).andReturn().getResponse().getContentAsString());
        double total = sale.get("total").asDouble();
        double half = Math.floor(total * 50) / 100; // avoid float drift
        double rest = total - half;

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(
                                        Map.of("method", "CASH", "amount", half),
                                        Map.of("method", "CARD", "amount", rest))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VALIDATED")));
    }

    private void openSession() throws Exception {
        mockMvc.perform(post("/api/pos/sessions/open")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 50))))
                .andExpect(status().isCreated());
    }

    private void openSessionAsCashier() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.CASHIER_EMAIL,
                TestAuthReferenceDataInitializer.CASHIER_PASSWORD);
        mockMvc.perform(post("/api/pos/sessions/open")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + adminToken);
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
