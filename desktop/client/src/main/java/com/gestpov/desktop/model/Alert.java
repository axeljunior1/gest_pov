package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record Alert(
        Long id,
        String type,
        String severity,
        String status,
        Long productId,
        String productNom,
        Long warehouseId,
        String warehouseCode,
        String message,
        BigDecimal triggeredValue,
        BigDecimal thresholdValue,
        String firstTriggeredAt,
        String lastTriggeredAt,
        Integer triggerCount
) {
    public static Alert fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Alert(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "type"),
                Product.textOrNull(node, "severity"),
                Product.textOrNull(node, "status"),
                Product.longOrNull(node, "productId"),
                Product.textOrNull(node, "productNom"),
                Product.longOrNull(node, "warehouseId"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "message"),
                Product.decimalOrNull(node, "triggeredValue"),
                Product.decimalOrNull(node, "thresholdValue"),
                Product.textOrNull(node, "firstTriggeredAt"),
                Product.textOrNull(node, "lastTriggeredAt"),
                node.hasNonNull("triggerCount") ? node.get("triggerCount").asInt() : null
        );
    }
}
