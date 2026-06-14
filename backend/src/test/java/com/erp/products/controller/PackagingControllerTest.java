package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.PackagingToBaseRequest;
import com.erp.products.dto.ProductPackagingRequest;
import com.erp.products.dto.ProductRequest;
import com.erp.products.dto.UnitOfMeasureRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PackagingControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldManageProductPackagingsAndConvertToBaseUnit() throws Exception {
        UnitOfMeasureRequest bouteille = new UnitOfMeasureRequest();
        bouteille.setNom("Bouteille");
        bouteille.setSymbole("btl");

        String unitResponse = mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bouteille)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long unitId = objectMapper.readTree(unitResponse).get("id").asLong();

        ProductRequest product = new ProductRequest();
        product.setNom("Eau minérale");
        product.setSku("EAU-001");
        product.setUnitId(unitId);
        product.setPrixVente(new BigDecimal("500"));
        product.setStatut(ProductStatus.ACTIF);
        product.setCycleVie(LifecycleStatus.ACTIF);

        String productResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productResponse).get("id").asLong();

        ProductPackagingRequest carton = new ProductPackagingRequest();
        carton.setNom("Carton");
        carton.setSymbole("ctn");
        carton.setQuantiteBase(new BigDecimal("12"));
        carton.setPrixVente(new BigDecimal("5500"));
        carton.setPrincipal(true);

        String packagingResponse = mockMvc.perform(post("/api/products/{id}/packagings", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carton)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantiteBase").value(12))
                .andExpect(jsonPath("$.prixVente").value(5500))
                .andExpect(jsonPath("$.baseUnitSymbole").value("btl"))
                .andReturn().getResponse().getContentAsString();
        Long packagingId = objectMapper.readTree(packagingResponse).get("id").asLong();

        ProductPackagingRequest palette = new ProductPackagingRequest();
        palette.setNom("Palette");
        palette.setQuantiteBase(new BigDecimal("600"));

        mockMvc.perform(post("/api/products/{id}/packagings", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(palette)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantiteBase").value(600));

        mockMvc.perform(get("/api/products/{id}/packagings", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        PackagingToBaseRequest convert = new PackagingToBaseRequest();
        convert.setPackagingId(packagingId);
        convert.setQuantity(new BigDecimal("3"));

        mockMvc.perform(post("/api/products/{id}/packagings/convert-to-base", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convert)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantiteBase").value(36))
                .andExpect(jsonPath("$.baseUnitSymbole").value("btl"))
                .andExpect(jsonPath("$.explanation").exists());
    }

    @Test
    void shouldRejectPackagingWithoutBaseUnit() throws Exception {
        ProductRequest product = new ProductRequest();
        product.setNom("Produit sans unité");
        product.setSku("NO-UNIT-001");
        product.setStatut(ProductStatus.ACTIF);
        product.setCycleVie(LifecycleStatus.ACTIF);

        String productResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productResponse).get("id").asLong();

        ProductPackagingRequest carton = new ProductPackagingRequest();
        carton.setNom("Carton");
        carton.setQuantiteBase(new BigDecimal("12"));

        mockMvc.perform(post("/api/products/{id}/packagings", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carton)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("unité de base")));
    }

    @Test
    void shouldFilterPackagingsByUsageContext() throws Exception {
        UnitOfMeasureRequest bouteille = new UnitOfMeasureRequest();
        bouteille.setNom("Piece");
        bouteille.setSymbole("pc");

        String unitResponse = mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bouteille)))
                .andReturn().getResponse().getContentAsString();
        Long unitId = objectMapper.readTree(unitResponse).get("id").asLong();

        ProductRequest product = new ProductRequest();
        product.setNom("Produit test condi");
        product.setSku("CONDI-CTX-001");
        product.setUnitId(unitId);
        product.setPrixVente(new BigDecimal("10"));
        product.setStatut(ProductStatus.ACTIF);
        product.setCycleVie(LifecycleStatus.ACTIF);

        String productResponse = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productResponse).get("id").asLong();

        ProductPackagingRequest purchaseOnly = new ProductPackagingRequest();
        purchaseOnly.setNom("Palette");
        purchaseOnly.setQuantiteBase(new BigDecimal("100"));
        purchaseOnly.setUsableForSale(false);
        purchaseOnly.setUsableForPurchase(true);
        purchaseOnly.setPrincipal(true);

        ProductPackagingRequest saleOnly = new ProductPackagingRequest();
        saleOnly.setNom("Unite client");
        saleOnly.setQuantiteBase(new BigDecimal("1"));
        saleOnly.setUsableForSale(true);
        saleOnly.setUsableForPurchase(false);
        saleOnly.setDefaultVente(true);

        mockMvc.perform(post("/api/products/{id}/packagings", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseOnly)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products/{id}/packagings", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleOnly)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/{id}/packagings", productId).param("context", "SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nom").value("Unite client"));

        mockMvc.perform(get("/api/products/{id}/packagings", productId).param("context", "PURCHASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nom").value("Palette"));
    }
}
