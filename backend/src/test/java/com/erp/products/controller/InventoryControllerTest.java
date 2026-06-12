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

class InventoryControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private Long packagingId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Inv Cat"))))
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

        String sku = "INV-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Inventaire",
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
        JsonNode product = objectMapper.readTree(productResult.getResponse().getContentAsString());
        productId = product.get("id").asLong();
        variantId = product.get("variantes").get(0).get("id").asLong();

        MvcResult pkgResult = mockMvc.perform(post("/api/products/" + productId + "/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Carton",
                                "symbole", "ctn",
                                "quantiteBase", 12,
                                "principal", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        packagingId = objectMapper.readTree(pkgResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "W" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot inv"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        warehouseId = objectMapper.readTree(wh.getResponse().getContentAsString()).get("id").asLong();

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
    void shouldCreateDraftInventoryWithLine() throws Exception {
        mockMvc.perform(post("/api/stock/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInventoryPayload(100))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.inventoryNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath("$.lignes[0].quantitySystem").value(0))
                .andExpect(jsonPath("$.createdBy").isNotEmpty());
    }

    @Test
    void shouldValidateWithZeroDifferenceWithoutInventoryMovement() throws Exception {
        seedStock(100);
        Long inventoryId = createInventory(100);

        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VALIDATED")))
                .andExpect(jsonPath("$.validatedBy").isNotEmpty())
                .andExpect(jsonPath("$.lignes[0].differenceQuantity").value(0));

        mockMvc.perform(get("/api/stock/movements")
                        .param("productId", productId.toString())
                        .param("type", "INVENTORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(100));
    }

    @Test
    void shouldValidateWithNegativeDifferenceAndDecreaseStock() throws Exception {
        seedStock(100);
        Long inventoryId = createInventory(96);

        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].differenceQuantity").value(-4));

        mockMvc.perform(get("/api/stock/movements")
                        .param("productId", productId.toString())
                        .param("type", "INVENTORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(96))
                .andExpect(jsonPath("$[0].referenceType", is("INVENTORY_COUNT")));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(96));
    }

    @Test
    void shouldValidateWithPositiveDifferenceAndIncreaseStock() throws Exception {
        seedStock(100);
        Long inventoryId = createInventory(110);

        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].differenceQuantity").value(10));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(110));
    }

    @Test
    void shouldConvertPackagingQuantityOnInventoryLine() throws Exception {
        seedStock(120);
        Long inventoryId = createInventoryWithPackaging(9);

        mockMvc.perform(get("/api/stock/inventories/" + inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].quantityCounted").value(108));

        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].differenceQuantity").value(-12));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(108));
    }

    @Test
    void shouldRejectUpdateWhenValidated() throws Exception {
        seedStock(50);
        Long inventoryId = createInventory(50);
        mockMvc.perform(post("/api/stock/inventories/" + inventoryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/stock/inventories/" + inventoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInventoryPayload(40))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDenyCreateForViewerWithPermission() throws Exception {
        String token = loginToken(TestAuthReferenceDataInitializer.VIEWER_EMAIL,
                TestAuthReferenceDataInitializer.VIEWER_PASSWORD);

        mockMvc.perform(post("/api/stock/inventories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInventoryPayload(10))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCreateForManagerWithPermission() throws Exception {
        String token = loginToken(TestAuthReferenceDataInitializer.MANAGER_EMAIL,
                TestAuthReferenceDataInitializer.MANAGER_PASSWORD);

        mockMvc.perform(post("/api/stock/inventories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInventoryPayload(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void shouldNotBreakStockEntryValidation() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 25
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VALIDATED")));

        mockMvc.perform(get("/api/stock/available")
                        .param("productId", productId.toString())
                        .param("variantId", variantId.toString())
                        .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(25));
    }

    private Map<String, Object> buildInventoryPayload(int countedQty) {
        return Map.of(
                "warehouseId", warehouseId,
                "locationId", locationId,
                "lignes", List.of(Map.of(
                        "productId", productId,
                        "variantId", variantId,
                        "quantityInput", countedQty
                ))
        );
    }

    private Long createInventory(int countedQty) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stock/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInventoryPayload(countedQty))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createInventoryWithPackaging(int cartonQty) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stock/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "packagingId", packagingId,
                                        "quantityInput", cartonQty
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void seedStock(int quantity) throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", quantity
                        ))))
                .andExpect(status().isCreated());
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
