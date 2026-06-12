package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImportExportControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long unitId;
    private String unitSymbole;
    private Long warehouseId;
    private Long locationId;
    private String warehouseCode;

    @BeforeEach
    void setUp() throws Exception {
        String symbole = "u" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Piece", "symbole", symbole))))
                .andExpect(status().isCreated());

        MvcResult unitList = mockMvc.perform(get("/api/units")).andReturn();
        JsonNode units = objectMapper.readTree(unitList.getResponse().getContentAsString());
        unitId = units.get(units.size() - 1).get("id").asLong();
        unitSymbole = units.get(units.size() - 1).get("symbole").asText();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "W" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot import"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();
        warehouseCode = objectMapper.readTree(wh.getResponse().getContentAsString()).get("code").asText();

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
    void shouldExportProductsAndStock() throws Exception {
        createProductViaApi("EXP-001");

        mockMvc.perform(get("/api/export/products").param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("products.csv")));

        mockMvc.perform(get("/api/export/stock").param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("stock.csv")));
    }

    @Test
    void shouldImportNewProduct() throws Exception {
        String symbole = unitSymbole;
        String csv = """
                sku;nom;description;marque;categorieNom;unitSymbole;prixAchat;prixVente;statut;cycleVie
                IMP-NEW-1;Produit Import;Desc;;;%s;5;10;ACTIF;ACTIF
                """.formatted(symbole);

        mockMvc.perform(multipart("/api/import/products/preview")
                        .file(csvFile("products.csv", csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validRows", is(1)))
                .andExpect(jsonPath("$.errorRows", is(0)));

        mockMvc.perform(multipart("/api/import/products/validate")
                        .file(csvFile("products.csv", csv))
                        .param("duplicateMode", "REJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.successRows", is(1)));

        mockMvc.perform(get("/api/products/sku/IMP-NEW-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("IMP-NEW-1")));
    }

    @Test
    void shouldRejectExistingSkuByDefault() throws Exception {
        createProductViaApi("IMP-DUP-1");
        String csv = """
                sku;nom;description;marque;categorieNom;unitSymbole;prixAchat;prixVente;statut;cycleVie
                IMP-DUP-1;Autre nom;;;;%s;5;10;ACTIF;ACTIF
                """.formatted(unitSymbole);

        mockMvc.perform(multipart("/api/import/products/preview")
                        .file(csvFile("products.csv", csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorRows", is(1)))
                .andExpect(jsonPath("$.lines[0].message", containsString("deja existant")));
    }

    @Test
    void shouldUpdateExistingSkuWhenConfigured() throws Exception {
        createProductViaApi("IMP-UPD-1");
        String csv = """
                sku;nom;description;marque;categorieNom;unitSymbole;prixAchat;prixVente;statut;cycleVie
                IMP-UPD-1;Nom Mis a Jour;;;;%s;5;10;ACTIF;ACTIF
                """.formatted(unitSymbole);

        mockMvc.perform(multipart("/api/import/products/validate")
                        .file(csvFile("products.csv", csv))
                        .param("duplicateMode", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.successRows", is(1)));

        mockMvc.perform(get("/api/products/sku/IMP-UPD-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom", is("Nom Mis a Jour")));
    }

    @Test
    void shouldReportInvalidLineError() throws Exception {
        String csv = """
                sku;nom;description;marque;categorieNom;unitSymbole;prixAchat;prixVente;statut;cycleVie
                ;Nom sans SKU;;;;u;5;10;ACTIF;ACTIF
                """;

        mockMvc.perform(multipart("/api/import/products/preview")
                        .file(csvFile("products.csv", csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorRows", is(1)))
                .andExpect(jsonPath("$.lines[0].status", is("ERROR")));
    }

    @Test
    void shouldImportInitialStockAndCreateMovement() throws Exception {
        Long productId = createProductViaApi("IMP-STK-1");
        JsonNode product = getProductBySku("IMP-STK-1");
        Long variantId = product.get("variantes").get(0).get("id").asLong();
        String csv = """
                productSku;variantSku;warehouseCode;locationCode;quantity;lotNumber;expiryDate
                IMP-STK-1;;%s;A1;25;;
                """.formatted(warehouseCode);

        mockMvc.perform(multipart("/api/import/initial-stock/validate")
                        .file(csvFile("stock.csv", csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.successRows", is(1)));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(25));

        mockMvc.perform(get("/api/stock/movements").param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].movementType", is("INITIAL_STOCK")));
    }

    @Test
    void shouldDenyImportExportForViewerWithoutPermission() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.VIEWER_EMAIL,
                TestAuthReferenceDataInitializer.VIEWER_PASSWORD);

        mockMvc.perform(get("/api/export/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/import/products/preview")
                        .file(csvFile("products.csv", "sku;nom\nX;Y"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotBreakStockOnFailedImport() throws Exception {
        Long productId = createProductViaApi("IMP-REG-1");
        JsonNode product = getProductBySku("IMP-REG-1");
        Long variantId = product.get("variantes").get(0).get("id").asLong();

        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 10,
                                "utilisateur", "Test"
                        ))))
                .andExpect(status().isCreated());

        String csv = """
                productSku;variantSku;warehouseCode;locationCode;quantity;lotNumber;expiryDate
                UNKNOWN;;WH-X;A1;5;;
                """;

        mockMvc.perform(multipart("/api/import/initial-stock/validate")
                        .file(csvFile("stock.csv", csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.errorRows", is(1)));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10));
    }

    private MockMultipartFile csvFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private Long createProductViaApi(String sku) throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Imp Cat " + sku))))
                .andExpect(status().isCreated())
                .andReturn();
        Long categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit " + sku,
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
                .andExpect(status().isCreated());
        return getProductBySku(sku).get("id").asLong();
    }

    private JsonNode getProductBySku(String sku) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/sku/" + sku)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
