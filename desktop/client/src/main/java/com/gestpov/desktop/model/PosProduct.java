package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record PosProduct(
        Long id,
        String nom,
        String sku,
        BigDecimal unitPrice,
        BigDecimal stockAvailable,
        boolean hasVariants,
        Long matchedVariantId
) {
    public static PosProduct fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new PosProduct(
                Product.longOrNull(node, "id"),
                node.path("nom").asText(""),
                Product.textOrNull(node, "sku"),
                Product.decimalOrNull(node, "unitPrice"),
                Product.decimalOrNull(node, "stockAvailable"),
                node.path("hasVariants").asBoolean(false),
                Product.longOrNull(node, "matchedVariantId")
        );
    }

    public static List<PosProduct> fromSearch(JsonNode node) {
        JsonNode products = node == null ? null : node.get("products");
        return com.gestpov.desktop.net.JsonLists.mapArray(products, PosProduct::fromJson);
    }
}
