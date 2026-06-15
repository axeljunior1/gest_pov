package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.domain.enums.BarcodeType;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.PriceType;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest extends AbstractIntegrationTest {

    private Long categoryId;
    private Long supplierId;
    private Long brandId;

    @BeforeEach
    void setUp() throws Exception {
        CategoryRequest category = new CategoryRequest();
        category.setNom("Sport");
        String catResponse = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andReturn().getResponse().getContentAsString();
        categoryId = objectMapper.readTree(catResponse).get("id").asLong();

        SupplierRequest supplier = new SupplierRequest();
        supplier.setNom("Nike");
        String supResponse = mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(supplier)))
                .andReturn().getResponse().getContentAsString();
        supplierId = objectMapper.readTree(supResponse).get("id").asLong();

        BrandRequest brand = new BrandRequest();
        brand.setNom("Nike Sport");
        String brandResponse = mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brand)))
                .andReturn().getResponse().getContentAsString();
        brandId = objectMapper.readTree(brandResponse).get("id").asLong();
    }

    @Test
    void shouldCreateProductWithVariants() throws Exception {
        ProductRequest request = buildProductRequest();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Chaussure Running Pro"))
                .andExpect(jsonPath("$.sku").value("RUN-PRO-001"))
                .andExpect(jsonPath("$.categorieNom").value("Sport"))
                .andExpect(jsonPath("$.variantes", hasSize(2)))
                .andExpect(jsonPath("$.cycleVie").value("BROUILLON"));
    }

    @Test
    void shouldCreateSimpleProductWithAutoEan() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setNom("Cahier A4");
        request.setPrixVente(new BigDecimal("3.50"));
        request.setStatut(ProductStatus.ACTIF);
        request.setCycleVie(LifecycleStatus.BROUILLON);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codeBarre").exists())
                .andExpect(jsonPath("$.codeBarre").value(matchesPattern("\\d{13}")));
    }

    @Test
    void shouldSearchProductsByQueryAndFilters() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products").param("query", "Running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nom").value("Chaussure Running Pro"));

        mockMvc.perform(get("/api/products").param("categorieId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldGetProductBySku() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/sku/RUN-PRO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("RUN-PRO-001"));
    }

    @Test
    void shouldUpdatePriceWithHistory() throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).get("id").asLong();

        PriceUpdateRequest priceUpdate = new PriceUpdateRequest();
        priceUpdate.setType(PriceType.VENTE);
        priceUpdate.setNouveauPrix(new BigDecimal("129.99"));
        priceUpdate.setUtilisateur("Paul");

        mockMvc.perform(patch("/api/products/{id}/price", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixVente").value(129.99));

        mockMvc.perform(get("/api/products/{id}/price-history", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].utilisateur").value("Paul"))
                .andExpect(jsonPath("$[0].nouveauPrix").value(129.99));
    }

    @Test
    void shouldUpdateLifecycleAndAudit() throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).get("id").asLong();

        LifecycleUpdateRequest lifecycle = new LifecycleUpdateRequest();
        lifecycle.setCycleVie(LifecycleStatus.EN_ATTENTE_VALIDATION);
        lifecycle.setUtilisateur("Admin");

        mockMvc.perform(patch("/api/products/{id}/lifecycle", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lifecycle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleVie").value("EN_ATTENTE_VALIDATION"));

        mockMvc.perform(get("/api/products/{id}/audit", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].action").exists());
    }

    @Test
    void shouldMoveProductCategory() throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).get("id").asLong();

        CategoryRequest newCat = new CategoryRequest();
        newCat.setNom("Running");
        String newCatResponse = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCat)))
                .andReturn().getResponse().getContentAsString();
        Long newCategoryId = objectMapper.readTree(newCatResponse).get("id").asLong();

        MoveCategoryRequest move = new MoveCategoryRequest();
        move.setCategorieId(newCategoryId);
        move.setUtilisateur("Marie");

        mockMvc.perform(patch("/api/products/{id}/category", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(move)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorieNom").value("Running"));
    }

    @Test
    void shouldAddVariantWithBarcode() throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).get("id").asLong();

        ProductVariantRequest variant = new ProductVariantRequest();
        variant.setCouleur("Blanc");
        variant.setTaille("L");
        variant.setSku("RUN-PRO-001-WH-L");
        variant.setPrix(new BigDecimal("99.99"));
        variant.setStock(15);
        variant.setGenerateBarcode(true);
        variant.setBarcodeType(BarcodeType.EAN13);

        mockMvc.perform(post("/api/products/{id}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variant)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("RUN-PRO-001-WH-L"))
                .andExpect(jsonPath("$.barcodeType").value("EAN13"))
                .andExpect(jsonPath("$.codeBarre").exists())
                .andExpect(jsonPath("$.codeBarre").value(org.hamcrest.Matchers.matchesPattern("\\d{13}")));
    }

    @Test
    void shouldRejectDuplicateVariantCouleurTaille() throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).get("id").asLong();

        ProductVariantRequest variant = new ProductVariantRequest();
        variant.setCouleur("Blanc");
        variant.setTaille("L");
        variant.setSku("RUN-PRO-001-WH-L");
        variant.setPrix(new BigDecimal("99.99"));
        variant.setStock(15);
        variant.setGenerateBarcode(true);

        mockMvc.perform(post("/api/products/{id}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variant)))
                .andExpect(status().isCreated());

        ProductVariantRequest duplicate = new ProductVariantRequest();
        duplicate.setCouleur("Blanc");
        duplicate.setTaille("L");
        duplicate.setSku("RUN-PRO-001-WH-L-2");
        duplicate.setPrix(new BigDecimal("99.99"));
        duplicate.setStock(5);

        mockMvc.perform(post("/api/products/{id}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("existante")));
    }

    @Test
    void shouldRejectDuplicateSku() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildProductRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("SKU")));
    }

    @Test
    void shouldGenerateProductAndVariantSkusWhenMissing() throws Exception {
        ProductRequest request = buildProductRequest();
        request.setSku(null);

        ProductVariantRequest v1 = new ProductVariantRequest();
        v1.setCouleur("Noir");
        v1.setTaille("M");
        v1.setSku(null);
        v1.setStock(5);

        ProductVariantRequest v2 = new ProductVariantRequest();
        v2.setCouleur("Rouge");
        v2.setTaille("L");
        v2.setSku(null);
        v2.setStock(3);

        request.setVariantes(List.of(v1, v2));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(containsString("RUNNING-PRO")))
                .andExpect(jsonPath("$.variantes[0].sku").value(containsString("NOIR-M")))
                .andExpect(jsonPath("$.variantes[0].prix").value(99.99))
                .andExpect(jsonPath("$.variantes[1].sku").value(containsString("ROUGE-L")));
    }

    private ProductRequest buildProductRequest() {
        ProductRequest request = new ProductRequest();
        request.setNom("Chaussure Running Pro");
        request.setSku("RUN-PRO-001");
        request.setDescription("Chaussure de running haute performance");
        request.setMarqueId(brandId);
        request.setCategorieId(categoryId);
        request.setPrixAchat(new BigDecimal("45.00"));
        request.setPrixVente(new BigDecimal("99.99"));
        request.setFournisseurPrincipalId(supplierId);
        request.setStatut(ProductStatus.ACTIF);
        request.setCycleVie(LifecycleStatus.BROUILLON);
        request.setUtilisateur("Admin");

        ProductVariantRequest v1 = new ProductVariantRequest();
        v1.setCouleur("Noir");
        v1.setTaille("M");
        v1.setSku("RUN-PRO-001-BK-M");
        v1.setPrix(new BigDecimal("99.99"));
        v1.setStock(25);
        v1.setGenerateBarcode(true);
        v1.setBarcodeType(BarcodeType.EAN13);
        v1.setCodeBarre("5901234123457");

        ProductVariantRequest v2 = new ProductVariantRequest();
        v2.setCouleur("Noir");
        v2.setTaille("L");
        v2.setSku("RUN-PRO-001-BK-L");
        v2.setPrix(new BigDecimal("99.99"));
        v2.setStock(10);
        v2.setGenerateBarcode(true);
        v2.setBarcodeType(BarcodeType.EAN13);

        request.setVariantes(List.of(v1, v2));

        ProductSupplierRequest ps = new ProductSupplierRequest();
        ps.setSupplierId(supplierId);
        ps.setPrincipal(true);
        ps.setReferenceFournisseur("NK-RUN-001");
        ps.setDelaiLivraisonJours(5);
        ps.setPrixNegocie(new BigDecimal("42.00"));
        request.setFournisseurs(List.of(ps));

        return request;
    }
}
