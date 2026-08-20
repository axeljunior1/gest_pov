package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record UnitConversion(
        Long id,
        Long fromUnitId,
        String fromUnitSymbole,
        Long toUnitId,
        String toUnitSymbole,
        BigDecimal factor
) {

    public static UnitConversion fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        BigDecimal factor = null;
        if (node.hasNonNull("factor")) {
            try {
                factor = new BigDecimal(node.get("factor").asText());
            } catch (NumberFormatException ignored) {
                // leave null
            }
        }
        return new UnitConversion(
                node.hasNonNull("id") ? node.get("id").asLong() : null,
                node.hasNonNull("fromUnitId") ? node.get("fromUnitId").asLong() : null,
                node.hasNonNull("fromUnitSymbole") ? node.get("fromUnitSymbole").asText() : "",
                node.hasNonNull("toUnitId") ? node.get("toUnitId").asLong() : null,
                node.hasNonNull("toUnitSymbole") ? node.get("toUnitSymbole").asText() : "",
                factor
        );
    }

    public String display() {
        return (fromUnitSymbole == null ? "?" : fromUnitSymbole)
                + " → " + (toUnitSymbole == null ? "?" : toUnitSymbole)
                + " × " + (factor == null ? "?" : factor.toPlainString());
    }
}
