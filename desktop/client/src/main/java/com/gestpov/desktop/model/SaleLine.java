package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record SaleLine(
        Long id,
        Long productId,
        String productNom,
        BigDecimal quantityInput,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineTotal,
        Boolean stockInsufficient
) {
    public static SaleLine fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new SaleLine(
                Product.longOrNull(node, "id"),
                Product.longOrNull(node, "productId"),
                Product.textOrNull(node, "productNom"),
                Product.decimalOrNull(node, "quantityInput"),
                Product.decimalOrNull(node, "unitPrice"),
                Product.decimalOrNull(node, "discountAmount"),
                Product.decimalOrNull(node, "lineTotal"),
                node.hasNonNull("stockInsufficient") && node.get("stockInsufficient").asBoolean()
        );
    }
}
