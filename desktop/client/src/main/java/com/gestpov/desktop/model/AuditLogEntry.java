package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditLogEntry(
        Long id,
        String action,
        String details,
        String utilisateur,
        String dateAction
) {

    public static AuditLogEntry fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String action = node.hasNonNull("action") ? node.get("action").asText() : "";
        return new AuditLogEntry(
                node.hasNonNull("id") ? node.get("id").asLong() : null,
                action,
                node.hasNonNull("details") ? node.get("details").asText() : "",
                node.hasNonNull("utilisateur") ? node.get("utilisateur").asText() : "",
                node.hasNonNull("dateAction") ? node.get("dateAction").asText() : ""
        );
    }
}
