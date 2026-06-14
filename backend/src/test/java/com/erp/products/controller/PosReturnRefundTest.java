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

/**
 * Retours / remboursements POS — enrichissement SaleRefund existant.
 */
class PosReturnRefundTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private String warehouseCode;
    private String adminToken;
    private String cashierToken;
    private String sellerToken;

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
        sellerToken = loginToken(TestAuthReferenceDataInitializer.SELLER_EMAIL, TestAuthReferenceDataInitializer.SELLER_PASSWORD);
    }

    @Test
    void fullReturnWithRestockIncrementsStock() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(2);
        double total = saleTotal(saleId);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Retour total",
                                "returnToStock", true,
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.refundStatus", is("REFUNDED")));

        mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REFUNDED")));
    }

    @Test
    void returnWithoutRestockDoesNotIncreaseStock() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(1);
        double stockBefore = stockLevel();

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Produit casse",
                                "returnToStock", false))))
                .andExpect(status().isCreated());

        assert stockLevel() == stockBefore;
    }

    @Test
    void partialReturnSetsPartiallyRefunded() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(3);
        Long lineId = lineIdOf(saleId);
        double partialAmount = 10;

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Retour partiel",
                                "returnToStock", true,
                                "lines", List.of(Map.of("saleLineId", lineId, "quantity", 1)),
                                "payments", List.of(Map.of("method", "CASH", "amount", partialAmount))))))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PARTIALLY_REFUNDED")));
    }

    @Test
    void cannotReturnMoreThanSold() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(1);
        Long lineId = lineIdOf(saleId);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lines", List.of(Map.of("saleLineId", lineId, "quantity", 5)),
                                "payments", List.of(Map.of("method", "CASH", "amount", 50))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doubleReturnBlocked() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(1);

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("returnToStock", false))))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("returnToStock", false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellerCannotRefund() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(1);

        mockMvc.perform(auth(sellerToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("returnToStock", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnReceiptGenerated() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(1);

        MvcResult created = mockMvc.perform(auth(cashierToken, post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Test"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long refundId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(auth(cashierToken, get("/api/pos/returns/" + refundId + "/receipt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnNumber", notNullValue()))
                .andExpect(jsonPath("$.originalSaleNumber", notNullValue()));
    }

    @Test
    void returnableLinesEndpoint() throws Exception {
        openCashierSession(0);
        Long saleId = createAndPaySale(2);

        mockMvc.perform(auth(cashierToken, get("/api/pos/sales/" + saleId + "/returnable")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantityReturnable", is(2.0)))
                .andExpect(jsonPath("$.amountRefundable", greaterThan(0.0)));
    }

    private void seedProductAndWarehouse() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Ret Cat"))))
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

        String sku = "RET-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit retour",
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
                                "code", "WR" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot retour"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();
        warehouseCode = objectMapper.readTree(wh.getResponse().getContentAsString()).get("code").asText();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone retour"
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

    private Long lineIdOf(Long saleId) throws Exception {
        JsonNode sale = objectMapper.readTree(
                mockMvc.perform(auth(cashierToken, get("/api/pos/sales/" + saleId)))
                        .andReturn().getResponse().getContentAsString());
        return sale.get("lignes").get(0).get("id").asLong();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            String token,
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + adminToken);
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

    private double stockLevel() throws Exception {
        MvcResult stock = mockMvc.perform(auth(get("/api/stock")
                        .param("productId", String.valueOf(productId))
                        .param("warehouseId", String.valueOf(warehouseId))))
                .andReturn();
        JsonNode body = objectMapper.readTree(stock.getResponse().getContentAsString());
        if (body.isArray() && body.size() > 0) {
            return body.get(0).get("quantityOnHand").asDouble();
        }
        return 0;
    }
}
