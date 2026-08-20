package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record StockItem(
        Long id,
        Long productId,
        String productNom,
        String warehouseCode,
        String locationCode,
        String unitSymbole,
        BigDecimal quantityOnHand,
        BigDecimal quantityAvailable
) {

    public static StockItem fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new StockItem(
                Product.longOrNull(node, "id"),
                Product.longOrNull(node, "productId"),
                Product.textOrNull(node, "productNom"),
                Product.textOrNull(node, "warehouseCode"),
                Product.textOrNull(node, "locationCode"),
                Product.textOrNull(node, "unitSymbole"),
                Product.decimalOrNull(node, "quantityOnHand"),
                Product.decimalOrNull(node, "quantityAvailable")
        );
    }
}
