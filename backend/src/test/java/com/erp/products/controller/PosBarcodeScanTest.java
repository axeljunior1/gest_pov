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

class PosBarcodeScanTest extends com.erp.products.AbstractIntegrationTest {

    private Long warehouseId;
    private String adminToken;
    private String sellerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginToken("admin@erp.local", "ErpAdmin2026!");
        sellerToken = loginToken(
                TestAuthReferenceDataInitializer.SELLER_EMAIL,
                TestAuthReferenceDataInitializer.SELLER_PASSWORD);

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "WB" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot scan"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/warehouses/" + warehouseId + "/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "DEFAULT",
                                "nom", "Zone scan"
                        ))))
                .andExpect(status().isCreated());

        setPosMode("SELLER_COLLECTS_PAYMENT");
        openSession();
    }

    @Test
    void shouldScanSimpleProductBarcode() throws Exception {
        String barcode = "2001234567890";
        Long productId = createSimpleProduct("Savon", barcode);

        mockMvc.perform(auth(get("/api/pos/catalog/search"))
                        .param("q", barcode)
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchType").value("EXACT_BARCODE"));

        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/scan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", barcode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Savon")))
                .andExpect(jsonPath("$.sale.lignes", hasSize(1)))
                .andExpect(jsonPath("$.lookup.type").value("PRODUCT"));
    }

    @Test
    void shouldScanVariantBarcode() throws Exception {
        String barcode = "2009876543210";
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Seau",
                                "sku", "SEA-" + UUID.randomUUID().toString().substring(0, 5),
                                "prixVente", 1500,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "variantes", List.of(Map.of(
                                        "couleur", "Rouge",
                                        "taille", "M",
                                        "prix", 1500,
                                        "codeBarre", barcode
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long productId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        Long saleId = createSale();
        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/scan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", barcode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lookup.type").value("VARIANT"))
                .andExpect(jsonPath("$.sale.lignes[0].variantId").isNumber());
    }

    @Test
    void shouldRejectDuplicateBarcodeAcrossLevels() throws Exception {
        String barcode = "2001112223334";
        createSimpleProduct("Produit A", barcode);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit B",
                                "sku", "PB-" + UUID.randomUUID().toString().substring(0, 5),
                                "prixVente", 100,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "codeBarre", barcode
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("déjà utilisé")));
    }

    @Test
    void shouldReturnNotFoundForUnknownBarcode() throws Exception {
        mockMvc.perform(auth(get("/api/pos/catalog/search"))
                        .param("q", "999888777666")
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchType").value("BARCODE_NOT_FOUND"));
    }

    @Test
    void shouldIncrementQuantityOnRepeatedScan() throws Exception {
        String barcode = "2005556667778";
        createSimpleProduct("Riz 1kg", barcode);
        Long saleId = createSale();

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/scan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", barcode))))
                .andExpect(status().isOk());

        mockMvc.perform(auth(post("/api/pos/sales/" + saleId + "/scan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", barcode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sale.lignes", hasSize(1)))
                .andExpect(jsonPath("$.sale.lignes[0].quantityInput").value(2.0));
    }

    private Long createSimpleProduct(String nom, String barcode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", nom,
                                "sku", "SKU-" + UUID.randomUUID().toString().substring(0, 6),
                                "prixVente", 500,
                                "statut", "ACTIF",
                                "cycleVie", "ACTIF",
                                "codeBarre", barcode
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void setPosMode(String mode) throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos_sales_flow_mode", mode)))))
                .andExpect(status().isOk());
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
