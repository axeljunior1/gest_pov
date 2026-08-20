package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ImportJob(
        Long id,
        String importType,
        String status,
        String fileName,
        String createdBy,
        int totalRows,
        int successRows,
        int errorRows,
        String createdAt,
        String completedAt
) {
    public static ImportJob fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new ImportJob(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "importType"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "fileName"),
                Product.textOrNull(node, "createdBy"),
                node.path("totalRows").asInt(0),
                node.path("successRows").asInt(0),
                node.path("errorRows").asInt(0),
                Product.textOrNull(node, "createdAt"),
                Product.textOrNull(node, "completedAt")
        );
    }
}
