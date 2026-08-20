package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record StockItem(
        Long id,
        Long productId,
        String productNom,
        Long warehouseId,
        String warehouseCode,
        Long locationId,
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
                Product.longOrNull(node, "warehouseId"),
                Product.textOrNull(node, "warehouseCode"),
                Product.longOrNull(node, "locationId"),
                Product.textOrNull(node, "locationCode"),
                Product.textOrNull(node, "unitSymbole"),
                Product.decimalOrNull(node, "quantityOnHand"),
                Product.decimalOrNull(node, "quantityAvailable")
        );
    }
}
