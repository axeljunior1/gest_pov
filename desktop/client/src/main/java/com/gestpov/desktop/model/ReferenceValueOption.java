package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ReferenceValueOption(String code, String label) {

    public static ReferenceValueOption fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String code = node.path("code").asText("");
        String label = node.hasNonNull("label") ? node.get("label").asText() : code;
        return new ReferenceValueOption(code, label);
    }

    @Override
    public String toString() {
        return label == null || label.isBlank() ? code : label;
    }
}
