package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ProductImage(Long id, String fileName, String url, boolean principale) {

    public static ProductImage fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? node.get("id").asLong() : null;
        String fileName = node.path("fileName").asText("");
        String url = node.hasNonNull("url") ? node.get("url").asText() : null;
        boolean principale = node.path("principale").asBoolean(false);
        return new ProductImage(id, fileName, url, principale);
    }
}
