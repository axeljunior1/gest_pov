package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.erp.products.settings.SettingKeys;
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

class CustomerLoyaltyTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private String adminToken;
    private String cashierToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Loyalty Cat"))))
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

        String sku = "LY-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Fidelite",
                                "sku", sku,
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 100,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of("sku", sku + "-V1", "stock", 0, "prix", 100))
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
                                "nom", "Entrepot Loyalty"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loc = mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "DEFAULT", "nom", "Zone"))))
                .andExpect(status().isCreated())
                .andReturn();
        locationId = objectMapper.readTree(loc.getResponse().getContentAsString()).get("id").asLong();

        seedStock(50);

        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        PosTestSupport.useSellerCollectsMode(mockMvc, objectMapper, adminToken);
        cashierToken = loginToken(TestAuthReferenceDataInitializer.CASHIER_EMAIL, TestAuthReferenceDataInitializer.CASHIER_PASSWORD);
        managerToken = loginToken(TestAuthReferenceDataInitializer.MANAGER_EMAIL, TestAuthReferenceDataInitializer.MANAGER_PASSWORD);
    }

    @Test
    void shouldValidateAnonymousSaleWithoutPoints() throws Exception {
        openSession();
        Long saleId = createSaleWithLine();
        validateSale(saleId);
        mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(nullValue()))
                .andExpect(jsonPath("$.loyaltyPointsEarned", is(0)));
    }

    @Test
    void shouldQuickCreateCustomerAndSearchByPhone() throws Exception {
        openSession();
        mockMvc.perform(auth(post("/api/pos/customers/quick"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lastName", "Dupont",
                                "firstName", "Marie",
                                "phone", "0612345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerNumber", notNullValue()));

        mockMvc.perform(auth(get("/api/pos/customers/search")).param("q", "0612345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName", is("Dupont")));
    }

    @Test
    void shouldEarnPointsOnValidatedSaleWithCustomer() throws Exception {
        openSession();
        Long customerId = createCustomer("Jean", "Martin", "0699887766");
        Long saleId = createSaleWithLine();
        assignCustomer(saleId, customerId);
        validateSale(saleId);

        mockMvc.perform(auth(get("/api/customers/" + customerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPoints", is(100)));

        mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                .andExpect(jsonPath("$.loyaltyPointsEarned", is(100)));
    }

    @Test
    void shouldNotEarnPointsWhenLoyaltyDisabled() throws Exception {
        mockMvc.perform(auth(put("/api/settings/" + SettingKeys.LOYALTY_ENABLED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "false"))))
                .andExpect(status().isOk());

        openSession();
        Long customerId = createCustomer("Paul", "Durand", "0611223344");
        Long saleId = createSaleWithLine();
        assignCustomer(saleId, customerId);
        validateSale(saleId);

        mockMvc.perform(auth(get("/api/customers/" + customerId)))
                .andExpect(jsonPath("$.loyaltyPoints", is(0)));

        mockMvc.perform(auth(put("/api/settings/" + SettingKeys.LOYALTY_ENABLED))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "true"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRedeemPointsOnSale() throws Exception {
        openSession();
        Long customerId = createCustomer("Alice", "Vente", "0600000001");
        mockMvc.perform(post("/api/customers/" + customerId + "/loyalty/adjust")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 500, "reason", "seed"))))
                .andExpect(status().isOk());

        Long saleId = createSaleWithLine();
        assignCustomer(saleId, customerId);

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/loyalty/redeem"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 200))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPointsRedeemed", is(200)))
                .andExpect(jsonPath("$.loyaltyDiscountAmount", is(10.0)))
                .andExpect(jsonPath("$.total", is(90.0)));

        validateSale(saleId);

        mockMvc.perform(auth(get("/api/customers/" + customerId)))
                .andExpect(jsonPath("$.loyaltyPoints", is(390)));
    }

    @Test
    void shouldRejectRedeemBelowMinimum() throws Exception {
        openSession();
        Long customerId = createCustomer("Bob", "Min", "0600000002");
        mockMvc.perform(post("/api/customers/" + customerId + "/loyalty/adjust")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 150))))
                .andExpect(status().isOk());

        Long saleId = createSaleWithLine();
        assignCustomer(saleId, customerId);

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/loyalty/redeem"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 50))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashierCannotAdjustPointsManagerCan() throws Exception {
        Long customerId = createCustomer("Eve", "RBAC", "0600000003");

        mockMvc.perform(post("/api/customers/" + customerId + "/loyalty/adjust")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 100))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/customers/" + customerId + "/loyalty/adjust")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPoints", is(100)));
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

    @Test
    void shouldReversePointsOnRefund() throws Exception {
        openSession();
        Long customerId = createCustomer("Luc", "Refund", "0600000004");
        Long saleId = createSaleWithLine();
        assignCustomer(saleId, customerId);
        validateSale(saleId);

        mockMvc.perform(auth(get("/api/customers/" + customerId)))
                .andExpect(jsonPath("$.loyaltyPoints", is(100)));

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/refund"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Retour"))))
                .andExpect(status().isCreated());

        mockMvc.perform(auth(get("/api/customers/" + customerId)))
                .andExpect(jsonPath("$.loyaltyPoints", is(0)));
    }

    private void openSession() throws Exception {
        mockMvc.perform(auth(post("/api/pos/sessions/open"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
    }

    private Long createSaleWithLine() throws Exception {
        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        Long saleId = objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "quantityInput", 1))))
                .andExpect(status().isOk());
        return saleId;
    }

    private Long createCustomer(String firstName, String lastName, String phone) throws Exception {
        MvcResult result = mockMvc.perform(auth(post("/api/customers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", firstName,
                                "lastName", lastName,
                                "phone", phone))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void assignCustomer(Long saleId, Long customerId) throws Exception {
        mockMvc.perform(auth(put("/api/pos/sales/" + saleId + "/customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("customerId", customerId))))
                .andExpect(status().isOk());
    }

    private void validateSale(Long saleId) throws Exception {
        JsonNode sale = objectMapper.readTree(
                mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        double total = sale.get("total").asDouble();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total)),
                                "cashReceived", total))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder auth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + adminToken);
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
