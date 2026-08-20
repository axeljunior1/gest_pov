package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record LicenseStatus(
        boolean valid,
        boolean activated,
        String reason,
        String licenseId,
        String client,
        String site,
        String issuedAt,
        String expiresAt,
        Long daysRemaining,
        Integer maxUsers,
        String installationId
) {
    public static LicenseStatus fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new LicenseStatus(
                node.path("valid").asBoolean(false),
                node.path("activated").asBoolean(false),
                Product.textOrNull(node, "reason"),
                Product.textOrNull(node, "licenseId"),
                Product.textOrNull(node, "client"),
                Product.textOrNull(node, "site"),
                Product.textOrNull(node, "issuedAt"),
                Product.textOrNull(node, "expiresAt"),
                node.hasNonNull("daysRemaining") ? node.get("daysRemaining").asLong() : null,
                node.hasNonNull("maxUsers") ? node.get("maxUsers").asInt() : null,
                Product.textOrNull(node, "installationId")
        );
    }
}
