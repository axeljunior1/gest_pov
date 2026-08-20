package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record ProductVariant(
        Long id,
        Long productId,
        String couleur,
        String taille,
        String label,
        String sku,
        BigDecimal prix,
        Integer stock,
        String codeBarre,
        Boolean active
) {

    public static ProductVariant fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String label = textOrNull(node, "label");
        if (label == null || label.isBlank()) {
            label = textOrNull(node, "name");
        }
        if (label == null || label.isBlank()) {
            String couleur = textOrNull(node, "couleur");
            String taille = textOrNull(node, "taille");
            label = ((couleur == null ? "" : couleur) + " " + (taille == null ? "" : taille)).trim();
        }
        return new ProductVariant(
                longOrNull(node, "id"),
                longOrNull(node, "productId"),
                textOrNull(node, "couleur"),
                textOrNull(node, "taille"),
                label,
                textOrNull(node, "sku"),
                decimalOrNull(node, "prix"),
                node.hasNonNull("stock") ? node.get("stock").asInt() : null,
                textOrNull(node, "codeBarre"),
                node.hasNonNull("active") ? node.get("active").asBoolean() : true
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
