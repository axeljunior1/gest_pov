package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record DashboardAlertSummary(
        long openAlerts,
        long openLowStock,
        long openOutOfStock,
        long openExpirySoon,
        long openExpired
) {
    public static DashboardAlertSummary fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new DashboardAlertSummary(
                node.path("openAlerts").asLong(0),
                node.path("openLowStock").asLong(0),
                node.path("openOutOfStock").asLong(0),
                node.path("openExpirySoon").asLong(0),
                node.path("openExpired").asLong(0)
        );
    }
}
