package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authentification Gest POV existante — POST /api/auth/login + GET /api/auth/me
 */
public class AuthClient {

    private final ApiClient api;

    public AuthClient(ApiClient api) {
        this.api = api;
    }

    public AuthSession login(String email, String password) throws ApiException {
        JsonNode json = api.post("/api/auth/login", api.loginPayload(email, password));
        AuthSession session = AuthSession.fromLogin(json);
        if (session.token() == null || session.token().isBlank()) {
            throw new ApiException("Réponse login sans jeton");
        }
        api.setBearerToken(session.token());
        return session;
    }

    public AuthSession loginWithBadge(String badgeCode, String pin) throws ApiException {
        JsonNode json = api.post("/api/auth/login/badge",
                java.util.Map.of("badgeCode", badgeCode, "pin", pin));
        AuthSession session = AuthSession.fromLogin(json);
        if (session.token() == null || session.token().isBlank()) {
            throw new ApiException("Réponse login sans jeton");
        }
        api.setBearerToken(session.token());
        return session;
    }

    public AuthSession me() throws ApiException {
        JsonNode json = api.get("/api/auth/me");
        return AuthSession.fromMe(api.getBearerToken(), json);
    }
}
