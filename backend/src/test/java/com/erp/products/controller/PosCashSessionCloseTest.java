package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.erp.products.domain.enums.CashDifferenceReason;
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

/**
 * Clôture caisse encaisseur — contrôle des écarts (session CASHIER).
 */
class PosCashSessionCloseTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private String adminToken;
    private String sellerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Close Cat"))))
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

        String sku = "CLOSE-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit close",
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
                                "code", "WC" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot close"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone close"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(50);
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        sellerToken = loginToken(TestAuthReferenceDataInitializer.SELLER_EMAIL, TestAuthReferenceDataInitializer.SELLER_PASSWORD);
        cashierToken = loginToken(TestAuthReferenceDataInitializer.CASHIER_EMAIL, TestAuthReferenceDataInitializer.CASHIER_PASSWORD);
        setPosMode("CENTRAL_CASHIER");
    }

    @Test
    void shouldRequireOpeningCashForCashierSession() throws Exception {
        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "CASHIER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closePreviewShowsExpectedCash() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(2, 20);

        mockMvc.perform(auth(cashierToken, get("/api/pos/sessions/current/close-preview")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingCashAmount", is(100.0)))
                .andExpect(jsonPath("$.cashRevenue", is(20.0)))
                .andExpect(jsonPath("$.expectedCashAmount", is(120.0)))
                .andExpect(jsonPath("$.differenceReasonOptions", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    void shouldCloseWithZeroDifference() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 110))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced", is(true)))
                .andExpect(jsonPath("$.differenceSeverity", is("BALANCED")))
                .andExpect(jsonPath("$.cashDifference", is(0.0)))
                .andExpect(jsonPath("$.closedBy", notNullValue()))
                .andExpect(jsonPath("$.cashierName", notNullValue()));
    }

    @Test
    void shouldRejectCloseWithoutDeclaredCash() throws Exception {
        openCashierSession(50);
        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireReasonForNegativeDifference() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 105))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Motif")));
    }

    @Test
    void shouldCloseWithNegativeDifferenceAndComment() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "closingCashAmount", 105,
                                "differenceReason", CashDifferenceReason.CHANGE_ERROR.name(),
                                "differenceComment", "Erreur rendu monnaie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashDifference", is(-5.0)))
                .andExpect(jsonPath("$.differenceSeverity", is("MINOR")))
                .andExpect(jsonPath("$.differenceReason", is("CHANGE_ERROR")))
                .andExpect(jsonPath("$.differenceComment", is("Erreur rendu monnaie")));
    }

    @Test
    void shouldCloseWithPositiveDifference() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "closingCashAmount", 115,
                                "differenceReason", CashDifferenceReason.COUNT_ERROR.name()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashDifference", is(5.0)))
                .andExpect(jsonPath("$.differenceSeverity", is("MINOR")));
    }

    @Test
    void cardPaymentDoesNotAffectExpectedCash() throws Exception {
        openCashierSession(100);
        openSalesAndPayCard(1, 25);

        mockMvc.perform(auth(cashierToken, get("/api/pos/sessions/current/close-preview")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardRevenue", is(25.0)))
                .andExpect(jsonPath("$.cashRevenue", is(0)))
                .andExpect(jsonPath("$.expectedCashAmount", is(100.0)));
    }

    @Test
    void cashRefundReducesExpectedCash() throws Exception {
        openCashierSession(100);
        Long saleId = openSalesAndPayCash(2, 20);

        mockMvc.perform(auth(adminToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "SALES",
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(adminToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Retour client",
                                "returnToStock", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(cashierToken, get("/api/pos/sessions/current/close-preview")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashRefundTotal", is(20.0)))
                .andExpect(jsonPath("$.expectedCashAmount", is(100.0)));
    }

    @Test
    void shouldRequireManagerValidationWhenEnabled() throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos.require_manager_validation_for_cash_difference", "true")))))
                .andExpect(status().isOk());

        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "closingCashAmount", 105,
                                "differenceReason", CashDifferenceReason.INPUT_ERROR.name()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("manager")));

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "closingCashAmount", 105,
                                "differenceReason", CashDifferenceReason.INPUT_ERROR.name(),
                                "managerEmail", "admin@erp.local",
                                "managerPassword", "ErpAdmin2026!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerValidatedBy", is("admin@erp.local")));
    }

    @Test
    void listClosedSessionsReturnsCashierSessions() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 110))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(adminToken, get("/api/pos/sessions/closed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].sessionNumber", notNullValue()))
                .andExpect(jsonPath("$[0].cashierName", notNullValue()))
                .andExpect(jsonPath("$[0].status", is("CLOSED")));
    }

    @Test
    void sessionReportAfterCloseContainsAuditFields() throws Exception {
        openCashierSession(100);
        openSalesAndPayCash(1, 10);

        MvcResult close = mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 110))))
                .andExpect(status().isOk())
                .andReturn();

        Long sessionId = objectMapper.readTree(close.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(auth(adminToken, get("/api/pos/sessions/" + sessionId + "/report")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionNumber", notNullValue()))
                .andExpect(jsonPath("$.expectedCashAmount", is(110.0)))
                .andExpect(jsonPath("$.declaredCashAmount", is(110.0)))
                .andExpect(jsonPath("$.closedAt", notNullValue()));
    }

    private void setPosMode(String mode) throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos_sales_flow_mode", mode)))))
                .andExpect(status().isOk());
    }

    private void openCashierSession(double openingCash) throws Exception {
        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "CASHIER",
                                "openingCashAmount", openingCash))))
                .andExpect(status().isCreated());
    }

    private void openSalesSession() throws Exception {
        mockMvc.perform(auth(sellerToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "SALES",
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private Long openSalesAndPayCash(int qty, double expectedTotal) throws Exception {
        openSalesSession();
        Long saleId = createDraftSale(qty);
        sendToPayment(saleId);
        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", expectedTotal)),
                                "cashReceived", expectedTotal))))
                .andExpect(status().isOk());
        return saleId;
    }

    private void openSalesAndPayCard(int qty, double expectedTotal) throws Exception {
        openSalesSession();
        Long saleId = createDraftSale(qty);
        sendToPayment(saleId);
        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CARD", "amount", expectedTotal))))))
                .andExpect(status().isOk());
    }

    private Long createDraftSale(int qty) throws Exception {
        MvcResult saleResult = mockMvc.perform(auth(sellerToken, post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", qty))))
                .andExpect(status().isOk());
        return saleId;
    }

    private void sendToPayment(Long saleId) throws Exception {
        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk());
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
