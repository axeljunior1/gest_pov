package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AppSetting(String key, String value, String description, String type) {

    public static AppSetting fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new AppSetting(
                node.path("key").asText(""),
                node.hasNonNull("value") ? node.get("value").asText() : "",
                node.hasNonNull("description") ? node.get("description").asText() : "",
                node.hasNonNull("type") ? node.get("type").asText() : "STRING"
        );
    }
}
