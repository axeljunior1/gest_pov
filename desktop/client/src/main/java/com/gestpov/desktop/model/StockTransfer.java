package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record StockTransfer(
        Long id,
        String reference,
        Long sourceWarehouseId,
        String sourceWarehouseCode,
        Long destWarehouseId,
        String destWarehouseCode,
        String status,
        String notes,
        String createdAt
) {

    public static StockTransfer fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new StockTransfer(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "reference"),
                Product.longOrNull(node, "sourceWarehouseId"),
                Product.textOrNull(node, "sourceWarehouseCode"),
                Product.longOrNull(node, "destWarehouseId"),
                Product.textOrNull(node, "destWarehouseCode"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "notes"),
                Product.textOrNull(node, "createdAt")
        );
    }
}
