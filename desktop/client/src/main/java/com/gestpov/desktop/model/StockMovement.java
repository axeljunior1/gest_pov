package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record StockMovement(
        Long id,
        String movementType,
        Long productId,
        String productNom,
        String warehouseCode,
        String locationCode,
        BigDecimal quantity,
        BigDecimal quantityAfter,
        String reference,
        String createdAt
) {

    public static StockMovement fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String when = Product.textOrNull(node, "createdAt");
        if (when == null) {
            when = Product.textOrNull(node, "movementDate");
        }
        return new StockMovement(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "movementType"),
                Product.longOrNull(node, "productId"),
                Product.textOrNull(node, "productNom"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.decimalOrNull(node, "quantity"),
                Product.decimalOrNull(node, "quantityAfter"),
                Product.textOrNull(node, "reference"),
                when
        );
    }

    public String typeLabel() {
        if (movementType == null) {
            return "—";
        }
        return switch (movementType) {
            case "RECEIPT", "IN", "ENTRY" -> "Réception";
            case "ISSUE", "OUT", "EXIT" -> "Sortie";
            case "ADJUST", "ADJUSTMENT" -> "Ajustement";
            default -> movementType;
        };
    }
}
