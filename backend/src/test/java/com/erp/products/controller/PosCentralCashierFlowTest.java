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

/**
 * Tests du flux caisse centrale (mode par defaut CENTRAL_CASHIER).
 */
class PosCentralCashierFlowTest extends com.erp.products.AbstractIntegrationTest {

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
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Central Cat"))))
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

        String productSku = "CTR-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit central",
                                "sku", productSku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 10,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", productSku + "-V1",
                                        "stock", 0,
                                        "prix", 10,
                                        "codeBarre", "CTRBAR" + UUID.randomUUID().toString().substring(0, 8)
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
                                "nom", "Entrepot central"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone central"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(30);
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        sellerToken = loginToken(TestAuthReferenceDataInitializer.SELLER_EMAIL, TestAuthReferenceDataInitializer.SELLER_PASSWORD);
        cashierToken = loginToken(TestAuthReferenceDataInitializer.CASHIER_EMAIL, TestAuthReferenceDataInitializer.CASHIER_PASSWORD);

        setPosMode("CENTRAL_CASHIER");
    }

    @Test
    void sellerCreatesDraftAndSendsToPaymentWithoutStockChange() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 2);

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(30.0));

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
                .andExpect(jsonPath("$.sellerId", notNullValue()))
                .andExpect(jsonPath("$.sentToPaymentAt", notNullValue()));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(30.0));
    }

    @Test
    void sellerCannotCollectPayment() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        sendToPayment(sellerToken, saleId);
        double total = getSaleTotal(sellerToken, saleId);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotCollectWithoutOpenSession() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        sendToPayment(sellerToken, saleId);
        double total = getSaleTotal(sellerToken, saleId);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashierCollectsCashAndDecrementsStock() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 3);
        sendToPayment(sellerToken, saleId);
        double total = getSaleTotal(sellerToken, saleId);

        openCashierSession(cashierToken);

        mockMvc.perform(auth(cashierToken, get("/api/pos/sales/pending-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total)),
                                "cashReceived", total))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.paidAt", notNullValue()))
                .andExpect(jsonPath("$.paymentSessionId", notNullValue()));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(27.0));

        mockMvc.perform(auth(cashierToken, get("/api/pos/sales/" + saleId + "/ticket")))
                .andExpect(status().isOk());
    }

    @Test
    void cashierCollectsMixedPayment() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 2);
        sendToPayment(sellerToken, saleId);
        double total = getSaleTotal(sellerToken, saleId);
        double half = Math.floor(total * 50) / 100;
        double rest = total - half;

        openCashierSession(cashierToken);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(
                                        Map.of("method", "CASH", "amount", half),
                                        Map.of("method", "CARD", "amount", rest))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.payments", hasSize(2)));
    }

    @Test
    void insufficientPaymentIsRejected() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        sendToPayment(sellerToken, saleId);
        openCashierSession(cashierToken);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", 0.01))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockIsRejectedAtPayment() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 50);
        sendToPayment(sellerToken, saleId);
        openCashierSession(cashierToken);
        double total = getSaleTotal(sellerToken, saleId);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doublePaymentIsRejected() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        sendToPayment(sellerToken, saleId);
        openCashierSession(cashierToken);
        double total = getSaleTotal(sellerToken, saleId);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CARD", "amount", total))))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CARD", "amount", total))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellerCannotOpenCashierSession() throws Exception {
        mockMvc.perform(auth(sellerToken, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "CASHIER",
                                "openingCashAmount", 100))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessionCloseComputesCashDifference() throws Exception {
        openSalesSession(sellerToken);
        Long saleId = createDraftSale(sellerToken, 1);
        sendToPayment(sellerToken, saleId);
        double total = getSaleTotal(sellerToken, saleId);
        openCashierSession(cashierToken, 100);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total)),
                                "cashReceived", total + 100))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(cashierToken, post("/api/pos/sessions/close"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("closingCashAmount", 100 + total))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedCashAmount", notNullValue()))
                .andExpect(jsonPath("$.cashDifference", is(0.0)));
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionType", is("SALES")));
    }

    private void openCashierSession(String token) throws Exception {
        openCashierSession(token, 50);
    }

    private void openCashierSession(String token, double openingCash) throws Exception {
        mockMvc.perform(auth(token, post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "sessionType", "CASHIER",
                                "openingCashAmount", openingCash))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionType", is("CASHIER")));
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

    private void sendToPayment(String token, Long saleId) throws Exception {
        mockMvc.perform(auth(token, post("/api/pos/sales/" + saleId + "/send-to-payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
    }

    private double getSaleTotal(String token, Long saleId) throws Exception {
        JsonNode sale = objectMapper.readTree(
                mockMvc.perform(auth(token, get("/api/pos/sales/" + saleId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        return sale.get("total").asDouble();
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
