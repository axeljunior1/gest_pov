package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record StockEntryDoc(
        Long id,
        String entryNumber,
        String supplierNom,
        String warehouseCode,
        String locationCode,
        String entryDate,
        String referenceDocument,
        String status
) {

    public static StockEntryDoc fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new StockEntryDoc(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "entryNumber"),
                Product.textOrNull(node, "supplierNom"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.textOrNull(node, "entryDate"),
                Product.textOrNull(node, "referenceDocument"),
                Product.textOrNull(node, "status")
        );
    }
}
