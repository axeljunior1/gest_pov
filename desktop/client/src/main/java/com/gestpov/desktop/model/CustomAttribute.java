package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record CustomAttribute(Long id, String code, String label, String type) {

    public static CustomAttribute fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new CustomAttribute(
                node.hasNonNull("id") ? node.get("id").asLong() : null,
                node.path("code").asText(""),
                node.path("label").asText(""),
                node.path("type").asText("")
        );
    }
}
