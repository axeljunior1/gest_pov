package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Supplier(Long id, String nom, String email, String telephone, String adresse) {

    public static Supplier fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? node.get("id").asLong() : null;
        return new Supplier(
                id,
                node.path("nom").asText(""),
                text(node, "email"),
                text(node, "telephone"),
                text(node, "adresse")
        );
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : "";
    }

    @Override
    public String toString() {
        return nom == null ? "" : nom;
    }
}
