package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.util.List;

public record UserAccount(
        Long id,
        String firstName,
        String lastName,
        String email,
        String badgeCode,
        Boolean isActive,
        String lastLoginAt,
        String createdAt,
        List<String> roles,
        List<String> permissions
) {
    public static UserAccount fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new UserAccount(
                Product.longOrNull(node, "id"),
                node.path("firstName").asText(""),
                node.path("lastName").asText(""),
                node.path("email").asText(""),
                Product.textOrNull(node, "badgeCode"),
                !node.has("isActive") || node.get("isActive").asBoolean(true),
                Product.textOrNull(node, "lastLoginAt"),
                Product.textOrNull(node, "createdAt"),
                JsonLists.mapArray(node.get("roles"), n -> n.asText("")),
                JsonLists.mapArray(node.get("permissions"), n -> n.asText(""))
        );
    }

    public String displayName() {
        return (firstName + " " + lastName).trim();
    }
}
