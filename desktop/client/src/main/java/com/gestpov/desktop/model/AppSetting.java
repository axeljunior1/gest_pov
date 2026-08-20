package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AppSetting(
        String key,
        String value,
        String description,
        String type,
        String referenceCategory
) {

    public static AppSetting fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String ref = null;
        if (node.hasNonNull("referenceCategory")) {
            ref = node.get("referenceCategory").asText();
            if (ref != null && ref.isBlank()) {
                ref = null;
            }
        }
        return new AppSetting(
                node.path("key").asText(""),
                node.hasNonNull("value") ? node.get("value").asText() : "",
                node.hasNonNull("description") ? node.get("description").asText() : "",
                node.hasNonNull("type") ? node.get("type").asText() : "STRING",
                ref
        );
    }

    public boolean isBoolean() {
        return "BOOLEAN".equalsIgnoreCase(type);
    }

    public boolean isNumber() {
        return "NUMBER".equalsIgnoreCase(type);
    }

    public boolean isJson() {
        return "JSON".equalsIgnoreCase(type);
    }

    public boolean hasReferenceList() {
        return referenceCategory != null && !referenceCategory.isBlank();
    }

    public String label() {
        return description == null || description.isBlank() ? key : description;
    }

    public AppSetting withValue(String newValue) {
        return new AppSetting(key, newValue == null ? "" : newValue, description, type, referenceCategory);
    }
}
