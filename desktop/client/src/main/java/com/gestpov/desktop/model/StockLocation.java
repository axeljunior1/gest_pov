package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record StockLocation(Long id, Long warehouseId, String code, String nom) {

    public static StockLocation fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new StockLocation(
                Product.longOrNull(node, "id"),
                Product.longOrNull(node, "warehouseId"),
                Product.textOrNull(node, "code"),
                Product.textOrNull(node, "nom")
        );
    }

    public String label() {
        String n = nom == null || nom.isBlank() ? code : nom;
        if (code == null || code.isBlank()) {
            return n == null ? "" : n;
        }
        if (n == null || n.equals(code)) {
            return code;
        }
        return code + " — " + n;
    }

    @Override
    public String toString() {
        return label();
    }
}
