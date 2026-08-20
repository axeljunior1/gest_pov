package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Permission(
        Long id,
        String code,
        String name,
        String description,
        String module
) {
    public static Permission fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Permission(
                Product.longOrNull(node, "id"),
                node.path("code").asText(""),
                node.path("name").asText(""),
                node.path("description").asText(""),
                node.path("module").asText("")
        );
    }
}
