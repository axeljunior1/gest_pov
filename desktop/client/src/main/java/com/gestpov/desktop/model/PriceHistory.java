package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record PriceHistory(
        Long id,
        String type,
        BigDecimal ancienPrix,
        BigDecimal nouveauPrix,
        String utilisateur,
        String dateModification
) {

    public static PriceHistory fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new PriceHistory(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "type"),
                Product.decimalOrNull(node, "ancienPrix"),
                Product.decimalOrNull(node, "nouveauPrix"),
                Product.textOrNull(node, "utilisateur"),
                Product.textOrNull(node, "dateModification")
        );
    }
}
