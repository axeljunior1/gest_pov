package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record ProductPackaging(
        Long id,
        Long productId,
        String nom,
        String symbole,
        BigDecimal quantiteBase,
        String codeBarre,
        BigDecimal prixVente,
        Boolean usableForSale,
        Boolean usableForPurchase,
        Boolean actif
) {

    public static ProductPackaging fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new ProductPackaging(
                longOrNull(node, "id"),
                longOrNull(node, "productId"),
                node.path("nom").asText(""),
                textOrNull(node, "symbole"),
                decimalOrNull(node, "quantiteBase"),
                textOrNull(node, "codeBarre"),
                decimalOrNull(node, "prixVente"),
                node.path("usableForSale").asBoolean(true),
                node.path("usableForPurchase").asBoolean(true),
                node.path("actif").asBoolean(true)
        );
    }

    private static Long longOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asLong() : null;
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        try {
            return new BigDecimal(node.get(field).asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
