package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PosPackagingSaleTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private Long unitPackagingId;
    private Long cartonPackagingId;
    private String unitBarcode;
    private String cartonBarcode;
    private String adminToken;
    private String sellerToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        sellerToken = loginToken(
                TestAuthReferenceDataInitializer.SELLER_EMAIL,
                TestAuthReferenceDataInitializer.SELLER_PASSWORD);
        managerToken = loginToken(
                TestAuthReferenceDataInitializer.MANAGER_EMAIL,
                TestAuthReferenceDataInitializer.MANAGER_PASSWORD);

        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Boissons"))))
                .andExpect(status().isCreated())
                .andReturn();
        Long categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asLong();

        String symbole = "btl" + UUID.randomUUID().toString().substring(0, 4);
        mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Bouteille", "symbole", symbole))))
                .andExpect(status().isCreated());

        MvcResult unitList = mockMvc.perform(get("/api/units")).andReturn();
        JsonNode units = objectMapper.readTree(unitList.getResponse().getContentAsString());
        Long unitId = units.get(units.size() - 1).get("id").asLong();

        unitBarcode = "EAU-UNIT-" + UUID.randomUUID().toString().substring(0, 8);
        cartonBarcode = "EAU-CTN-" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Eau 1L",
                                "sku", "EAU-" + UUID.randomUUID().toString().substring(0, 6),
                                "categorieId", categoryId,
                                "unitId", unitId,
                                "prixVente", 500,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "sku", "EAU-V1",
                                        "stock", 0,
                                        "prix", 500,
                                        "codeBarre", unitBarcode
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        productId = objectMapper.readTree(productResult.getResponse().getContentAsString()).get("id").asLong();
        variantId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .get("variantes").get(0).get("id").asLong();

        unitPackagingId = createPackaging("Unité", 1, 500, null, true, false);
        cartonPackagingId = createPackaging("Carton", 12, 5500, cartonBarcode, false, false);
        createPackaging("Palette", 600, 250000, null, false, false);

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

        seedStock(1000);
        setPosMode("SELLER_COLLECTS_PAYMENT");
        openSession();
    }

    private void setPosMode(String mode) throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos_sales_flow_mode", mode)))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUseDefaultPackagingPriceOnCreate() throws Exception {
        mockMvc.perform(post("/api/products/" + productId + "/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Pack de 6",
                                "quantiteBase", 6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixVente").value(3000.0));
    }

    @Test
    void shouldSellSinglePackagingProduct() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", unitPackagingId,
                                "quantityInput", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].packagingId").value(unitPackagingId.intValue()))
                .andExpect(jsonPath("$.lignes[0].unitPrice").value(500.0))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(1000.0));
    }

    @Test
    void shouldExposeMultiplePackagingsInCatalog() throws Exception {
        mockMvc.perform(auth(get("/api/pos/catalog/products/" + productId).param("warehouseId", warehouseId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packagings", hasSize(3)));
    }

    @Test
    void shouldSellUnitPackaging() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", unitPackagingId,
                                "quantityInput", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].packagingNameSnapshot").value("Unité"))
                .andExpect(jsonPath("$.lignes[0].quantityInBaseUnit").value(3.0))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(1500.0));
    }

    @Test
    void shouldSellCartonWithOwnPrice() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].unitPrice").value(5500.0))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(11000.0))
                .andExpect(jsonPath("$.lignes[0].quantityInBaseUnit").value(24.0));
    }

    @Test
    void shouldSearchUnitBarcodeWithoutPackagingMatch() throws Exception {
        mockMvc.perform(auth(get("/api/pos/catalog/search"))
                        .param("q", unitBarcode)
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchType").value("EXACT_BARCODE"));
    }

    @Test
    void shouldSearchCartonBarcodeAndMatchPackaging() throws Exception {
        mockMvc.perform(auth(get("/api/pos/catalog/search"))
                        .param("q", cartonBarcode)
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchType").value("EXACT_PACKAGING_BARCODE"))
                .andExpect(jsonPath("$.products[0].matchedPackagingId").value(cartonPackagingId.intValue()));
    }

    @Test
    void shouldAddCartonLineWhenScanningPackagingBarcode() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].packagingNameSnapshot").value("Carton"))
                .andExpect(jsonPath("$.lignes[0].quantityInBaseUnit").value(12.0))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(5500.0));
    }

    @Test
    void shouldDecrementStockInBaseUnitOnPayment() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 3))))
                .andExpect(status().isOk());

        double total = objectMapper.readTree(
                        mockMvc.perform(auth(get("/api/pos/sales/" + saleId))).andReturn().getResponse().getContentAsString())
                .get("total").asDouble();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total))))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(1000.0 - 36.0));
    }

    @Test
    void shouldKeepUnitAndCartonLinesSeparate() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 2))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", unitPackagingId,
                                "quantityInput", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes", hasSize(2)))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(11000.0))
                .andExpect(jsonPath("$.lignes[1].lineTotal").value(1500.0));
    }

    @Test
    void shouldPreservePriceSnapshotAfterPackagingPriceChange() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].unitPriceSnapshot").value(5500.0));

        mockMvc.perform(put("/api/products/" + productId + "/packagings/" + cartonPackagingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Carton",
                                "quantiteBase", 12,
                                "prixVente", 6000))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(get("/api/pos/sales/" + saleId)))
                .andExpect(jsonPath("$.lignes[0].unitPriceSnapshot").value(5500.0))
                .andExpect(jsonPath("$.lignes[0].lineTotal").value(5500.0));
    }

    @Test
    void shouldRejectPackagingPriceUpdateWithoutPermission() throws Exception {
        mockMvc.perform(put("/api/products/" + productId + "/packagings/" + cartonPackagingId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Carton",
                                "quantiteBase", 12,
                                "prixVente", 9999))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Permission")));
    }

    @Test
    void shouldIgnoreSellerPriceOverrideOnSaleLine() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(sellerAuth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", cartonPackagingId,
                                "quantityInput", 1,
                                "unitPrice", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].unitPrice").value(5500.0));
    }

    @Test
    void shouldNotRegressStockEntryWithPackaging() throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "packagingId", cartonPackagingId,
                                "packagingQuantity", 2,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(1000.0 + 24.0));
    }

    @Test
    void shouldNotRegressPosPaymentFlow() throws Exception {
        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/lines"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "packagingId", unitPackagingId,
                                "quantityInput", 1))))
                .andExpect(status().isOk());

        double total = objectMapper.readTree(
                        mockMvc.perform(auth(get("/api/pos/sales/" + saleId))).andReturn().getResponse().getContentAsString())
                .get("total").asDouble();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/validate"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payments", List.of(Map.of("method", "CASH", "amount", total)),
                                "cashReceived", total + 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.changeAmount").value(100.0));
    }

    private Long createPackaging(
            String nom,
            int quantiteBase,
            int prixVente,
            String barcode,
            boolean defaultVente,
            boolean defaultAchat) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("nom", nom);
        body.put("quantiteBase", quantiteBase);
        body.put("prixVente", prixVente);
        if (barcode != null) {
            body.put("codeBarre", barcode);
        }
        body.put("defaultVente", defaultVente);
        body.put("defaultAchat", defaultAchat);
        MvcResult result = mockMvc.perform(post("/api/products/" + productId + "/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createSale() throws Exception {
        MvcResult saleResult = mockMvc.perform(auth(post("/api/pos/sales")))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(saleResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder sellerAuth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + sellerToken);
    }

    private void openSession() throws Exception {
        mockMvc.perform(post("/api/pos/sessions/open")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "openingCashAmount", 0))))
                .andExpect(status().isCreated());
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
}
