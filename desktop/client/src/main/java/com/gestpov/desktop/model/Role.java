package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.util.List;

public record Role(
        Long id,
        String name,
        String code,
        String description,
        Boolean isSystem,
        List<String> permissions
) {
    public static Role fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Role(
                Product.longOrNull(node, "id"),
                node.path("name").asText(""),
                node.path("code").asText(""),
                node.path("description").asText(""),
                node.hasNonNull("isSystem") && node.get("isSystem").asBoolean(),
                JsonLists.mapArray(node.get("permissions"), n -> n.asText(""))
        );
    }
}
