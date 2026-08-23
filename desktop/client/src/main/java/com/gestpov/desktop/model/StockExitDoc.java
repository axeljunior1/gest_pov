package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record StockExitDoc(
        Long id,
        String exitNumber,
        String warehouseCode,
        String locationCode,
        String exitDate,
        String reason,
        String status,
        String createdAt
) {

    public static StockExitDoc fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new StockExitDoc(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "exitNumber"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.textOrNull(node, "exitDate"),
                Product.textOrNull(node, "reason"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "createdAt")
        );
    }
}
