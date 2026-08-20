package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public record AuthSession(
        String token,
        String tokenType,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        List<String> permissions
) {
    public static AuthSession fromLogin(JsonNode json) {
        JsonNode user = json.path("user");
        return new AuthSession(
                json.path("token").asText(null),
                json.path("tokenType").asText("Bearer"),
                user.path("email").asText(""),
                user.path("firstName").asText(""),
                user.path("lastName").asText(""),
                stringList(user.path("roles")),
                stringList(json.has("permissions") ? json.path("permissions") : user.path("permissions"))
        );
    }

    public static AuthSession fromMe(String token, JsonNode json) {
        return new AuthSession(
                token,
                "Bearer",
                json.path("email").asText(""),
                json.path("firstName").asText(""),
                json.path("lastName").asText(""),
                stringList(json.path("roles")),
                stringList(json.path("permissions"))
        );
    }

    public String displayName() {
        String full = (firstName + " " + lastName).trim();
        return full.isBlank() ? email : full;
    }

    private static List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }
}
