package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

public record Customer(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String companyName,
        String address,
        String city,
        Integer loyaltyPoints
) {

    public static Customer fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Customer(
                node.hasNonNull("id") ? node.get("id").asLong() : null,
                node.path("firstName").asText(""),
                node.path("lastName").asText(""),
                node.path("phone").asText(""),
                node.path("email").asText(""),
                node.path("companyName").asText(""),
                node.path("address").asText(""),
                node.path("city").asText(""),
                node.hasNonNull("loyaltyPoints") ? node.get("loyaltyPoints").asInt() : 0
        );
    }

    public String displayName() {
        return (firstName + " " + lastName).trim();
    }
}
