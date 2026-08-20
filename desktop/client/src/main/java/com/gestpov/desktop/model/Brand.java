package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * DTO client — miroir de BrandResponse. Pas de règle métier.
 */
public record Brand(Long id, String nom, String createdAt, String updatedAt) {

    public static Brand fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? node.get("id").asLong() : null;
        String nom = node.path("nom").asText("");
        String created = textOrNull(node, "createdAt");
        String updated = textOrNull(node, "updatedAt");
        return new Brand(id, nom, created, updated);
    }

    private static String textOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
    }
}
