package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ImportPreview(int totalRows, int validRows, int errorRows) {
    public static ImportPreview fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return new ImportPreview(0, 0, 0);
        }
        return new ImportPreview(
                node.path("totalRows").asInt(0),
                node.path("validRows").asInt(0),
                node.path("errorRows").asInt(0)
        );
    }
}
