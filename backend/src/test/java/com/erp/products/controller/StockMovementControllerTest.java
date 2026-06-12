package com.erp.products.controller;

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

class StockMovementControllerTest extends com.erp.products.AbstractIntegrationTest {

    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult catResult = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nom", "Mvt Cat"))))
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

        String sku = "MVT-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nom", "Produit Mouvement",
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

        MvcResult wh = mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "W" + UUID.randomUUID().toString().substring(0, 6),
                                "nom", "Entrepot mvt"
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
    void shouldCreateInMovementWhenEntryValidated() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 40
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/movements")
                        .param("productId", productId.toString())
                        .param("type", "IN")
                        .param("referenceType", "STOCK_ENTRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movementType", is("IN")))
                .andExpect(jsonPath("$[0].quantityBefore").value(0))
                .andExpect(jsonPath("$[0].quantityAfter").value(40))
                .andExpect(jsonPath("$[0].referenceType", is("STOCK_ENTRY")))
                .andExpect(jsonPath("$[0].referenceId").value(entryId))
                .andExpect(jsonPath("$[0].stockEntryId").value(entryId))
                .andExpect(jsonPath("$[0].createdBy").isNotEmpty());
    }

    @Test
    void shouldCreateOutMovementWhenExitValidated() throws Exception {
        seedStockViaEntry(100);

        MvcResult createResult = mockMvc.perform(post("/api/stock/exits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "reason", "SALE",
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", 25
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long exitId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/stock/exits/" + exitId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/movements")
                        .param("referenceType", "STOCK_EXIT")
                        .param("referenceId", exitId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movementType", is("OUT")))
                .andExpect(jsonPath("$[0].quantityBefore").value(100))
                .andExpect(jsonPath("$[0].quantityAfter").value(75))
                .andExpect(jsonPath("$[0].stockExitId").value(exitId))
                .andExpect(jsonPath("$[0].referenceId").value(exitId));
    }

    @Test
    void shouldFilterMovementsByWarehouseAndType() throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 10,
                                "referenceType", "MANUAL",
                                "reference", "REC-MAN",
                                "utilisateur", "tester@erp.local"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock/movements")
                        .param("warehouseId", warehouseId.toString())
                        .param("type", "IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].warehouseId").value(warehouseId))
                .andExpect(jsonPath("$[0].movementType", is("IN")));
    }

    @Test
    void shouldGetMovementDetail() throws Exception {
        MvcResult receipt = mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 5
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long movementId = objectMapper.readTree(receipt.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/stock/movements/" + movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movementId))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.unitSymbole").isNotEmpty());
    }

    @Test
    void shouldExportMovementsAsCsv() throws Exception {
        mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 3
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/stock/movements/export")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("stock-movements.csv")))
                .andExpect(content().string(containsString("id;date;type")));
    }

    @Test
    void shouldRejectUpdateAndDeleteOnMovements() throws Exception {
        MvcResult receipt = mockMvc.perform(post("/api/stock/receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", productId,
                                "variantId", variantId,
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "quantityBase", 1
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long movementId = objectMapper.readTree(receipt.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/stock/movements/" + movementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/stock/movements/" + movementId))
                .andExpect(status().isMethodNotAllowed());
    }

    private void seedStockViaEntry(int quantity) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/stock/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationId", locationId,
                                "lignes", List.of(Map.of(
                                        "productId", productId,
                                        "variantId", variantId,
                                        "quantityInput", quantity
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/stock/entries/" + entryId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
