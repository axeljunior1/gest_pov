package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests ciblés sur « Envoyer à la caisse » (DRAFT → PENDING_PAYMENT).
 */
class PosSendToPaymentTest extends com.erp.products.AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;
    private Long warehouseId;
    private Long locationId;
    private String adminToken;
    private String sellerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Send Cat"))))
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

        String productSku = "SND-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit send",
                                "sku", productSku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 12,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", productSku + "-V1",
                                        "stock", 0,
                                        "prix", 12,
                                        "codeBarre", "SNDBAR" + UUID.randomUUID().toString().substring(0, 8)
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();
        Long variantId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("variantes").get(0).get("id").asLong();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "WS" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot send"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone send"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(variantId, 50);
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        sellerToken = loginToken(TestAuthReferenceDataInitializer.SELLER_EMAIL, TestAuthReferenceDataInitializer.SELLER_PASSWORD);
        cashierToken = loginToken(TestAuthReferenceDataInitializer.CASHIER_EMAIL, TestAuthReferenceDataInitializer.CASHIER_PASSWORD);
        setPosMode("CENTRAL_CASHIER");
    }

    @Test
    void shouldSendDraftSaleToPaymentQueueWithSellerAndTimestamp() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 2);

        MvcResult result = mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$.sellerId", notNullValue()))
                .andExpect(jsonPath("$.submittedAt", notNullValue()))
                .andExpect(jsonPath("$.sentToPaymentAt", notNullValue()))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(body.get("sellerId"));
        assertNotNull(body.get("submittedAt"));

        Long sellerIdInDb = jdbcTemplate.queryForObject(
                "SELECT seller_id FROM sales WHERE id = ?", Long.class, saleId);
        assertNotNull(sellerIdInDb);
    }

    @Test
    void shouldRejectSendToPaymentWhenCartIsEmpty() throws Exception {
        openSalesSession(sellerToken);
        MvcResult saleResult = mockMvc.perform(auth(sellerToken, post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Panier vide")));
    }

    @Test
    void shouldRejectSendToPaymentWhenNotCentralMode() throws Exception {
        setPosMode("SELLER_COLLECTS_PAYMENT");
        openCashierSessionAsSellerCollects(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("caisse centrale")));
    }

    @Test
    void shouldRejectSendToPaymentForHoldSaleUntilResumed() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/hold"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Client B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HOLD")));

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("brouillon")));
    }

    @Test
    void shouldSendAfterHoldResume() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/hold"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Pause"))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/resume")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
    }

    @Test
    void sentSaleShouldAppearInCashierPendingQueue() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 3);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk());

        openCashierSession(cashierToken);

        mockMvc.perform(auth(cashierToken, get("/api/pos/sales/pending-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(saleId.intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$[0].sellerName", notNullValue()));
    }

    @Test
    void shouldSendToPaymentViaSubmitPaymentAlias() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/submit-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
    }

    @Test
    void shouldRecallPendingPaymentToHoldAndAllowAddingLineAfterResume() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));

        openCashierSession(cashierToken);
        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/recall-from-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HOLD")))
                .andExpect(jsonPath("$.holdLabel", containsString("Retour caisse")))
                .andExpect(jsonPath("$.submittedAt").doesNotExist());

        mockMvc.perform(auth(cashierToken, get("/api/pos/sales/pending-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(auth(sellerToken, get("/api/pos/sales/hold")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(saleId.intValue())));

        mockMvc.perform(auth(sellerToken, get("/api/pos/sales/draft")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/resume")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.lignes", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void newSaleShouldAlwaysHaveSellerId() throws Exception {
        openSalesSession(sellerToken);
        MvcResult saleResult = mockMvc.perform(auth(sellerToken, post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerId", notNullValue()))
                .andReturn();

        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();
        Long sellerId = jdbcTemplate.queryForObject("SELECT seller_id FROM sales WHERE id = ?", Long.class, saleId);
        assertNotNull(sellerId);
    }

    private void setPosMode(String mode) throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos_sales_flow_mode", mode)))))
                .andExpect(status().isOk());
    }

    private void openSalesSession(String token) throws Exception {
        mockMvc.perform(auth(token, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "SALES",
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private void openCashierSession(String token) throws Exception {
        mockMvc.perform(auth(token, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "CASHIER",
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private void openCashierSessionAsSellerCollects(String token) throws Exception {
        mockMvc.perform(auth(token, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private Long createDraftSale(String token, int qty) throws Exception {
        MvcResult saleResult = mockMvc.perform(auth(token, post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(token, post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", qty))))
                .andExpect(status().isOk());
        return saleId;
    }

    private void seedStock(Long variantId, int quantity) throws Exception {
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
