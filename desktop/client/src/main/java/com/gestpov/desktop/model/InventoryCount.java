package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record InventoryCount(
        Long id,
        String inventoryNumber,
        String reference,
        String warehouseCode,
        String locationCode,
        String status,
        String createdAt
) {

    public static InventoryCount fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new InventoryCount(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "inventoryNumber"),
                Product.textOrNull(node, "reference"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "createdAt")
        );
    }
}
