package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Unit(Long id, String nom, String symbole) {

    public static Unit fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? node.get("id").asLong() : null;
        return new Unit(id, node.path("nom").asText(""), node.path("symbole").asText(""));
    }

    @Override
    public String toString() {
        if (symbole == null || symbole.isBlank()) {
            return nom == null ? "" : nom;
        }
        return nom + " (" + symbole + ")";
    }
}
