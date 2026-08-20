package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Warehouse(Long id, String code, String nom) {

    public static Warehouse fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Warehouse(
                Product.longOrNull(node, "id"),
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
