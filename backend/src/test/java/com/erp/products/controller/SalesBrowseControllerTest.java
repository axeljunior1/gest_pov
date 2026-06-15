package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.erp.products.support.PosTestSupport;
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

class SalesBrowseControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private String warehouseCode;
    private String adminToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        seedProductAndWarehouse();
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        PosTestSupport.useSellerCollectsMode(mockMvc, objectMapper, adminToken);
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos.default_warehouse_code", warehouseCode)))))
                .andExpect(status().isOk());
        cashierToken = loginToken(TestAuthReferenceDataInitializer.CASHIER_EMAIL, TestAuthReferenceDataInitializer.CASHIER_PASSWORD);
    }

    @Test
    void browseSalesAndDetailWithLinkedReturn() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(2);
        double total = saleTotal(saleId);

        MvcResult refundResult = mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Retour test browse",
                                "returnToStock", true,
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isCreated())
                .andReturn();
        Long returnId = objectMapper.readTree(refundResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(adminToken, get("/api/sales/browse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[?(@.id == " + saleId + ")].refundCount", hasItem(greaterThanOrEqualTo(1))));

        mockMvc.perform(auth(adminToken, get("/api/sales/browse/export")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("ventes.csv")));

        mockMvc.perform(auth(adminToken, get("/api/sales/" + saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sale.id", is(saleId.intValue())))
                .andExpect(jsonPath("$.sale.saleNumber", notNullValue()))
                .andExpect(jsonPath("$.refunds", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.refunds[0].id", is(returnId.intValue())))
                .andExpect(jsonPath("$.timeline[?(@.eventType == 'PAYMENT_VALIDATED')]", not(empty())));

        mockMvc.perform(auth(adminToken, get("/api/sales/returns/browse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + returnId + ")].saleId", hasItem(saleId.intValue())))
                .andExpect(jsonPath("$.items[?(@.id == " + returnId + ")].lineCount", hasItem(greaterThanOrEqualTo(1))));

        mockMvc.perform(auth(adminToken, get("/api/sales/returns/browse/export")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("retours.csv")));

        mockMvc.perform(auth(adminToken, get("/api/sales/returns/" + returnId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(returnId.intValue())))
                .andExpect(jsonPath("$.saleId", is(saleId.intValue())))
                .andExpect(jsonPath("$.saleNumber", notNullValue()))
                .andExpect(jsonPath("$.lignes", not(empty())));
    }

    private void seedProductAndWarehouse() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Browse Cat"))))
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

        String sku = "BRW-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit browse",
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
        productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();
        variantId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("variantes").get(0).get("id").asLong();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "WB" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot browse"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();
        warehouseCode = objectMapper.readTree(wh.getResponse().getContentAsString()).get("code").asText();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone browse"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 100,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());
    }

    private void openCashierSession(double opening) throws Exception {
        mockMvc.perform(auth(adminToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", opening))))
                .andExpect(status().isCreated());
    }

    private Long createAndPaySale(int qty) throws Exception {
        MvcResult saleResult = mockMvc.perform(auth(adminToken, post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(adminToken, post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", qty))))
                .andExpect(status().isOk());

        JsonNode sale = objectMapper.readTree(
                mockMvc.perform(auth(adminToken, get("/api/pos/sales/" + saleId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        double total = sale.get("total").asDouble();

        mockMvc.perform(auth(adminToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total)),
                                "cashReceived", total))))
                .andExpect(status().isOk());

        return saleId;
    }

    private double saleTotal(Long saleId) throws Exception {
        JsonNode sale = objectMapper.readTree(
                mockMvc.perform(auth(cashierToken, get("/api/pos/sales/" + saleId)))
                        .andReturn().getResponse().getContentAsString());
        return sale.get("total").asDouble();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            String token,
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
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
