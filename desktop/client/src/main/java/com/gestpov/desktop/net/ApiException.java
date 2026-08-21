package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiException extends Exception {

    private static final ObjectMapper BODY_MAPPER = new ObjectMapper();

    private final int statusCode;
    private final String responseBody;

    public ApiException(String message) {
        this(message, 0, null, null);
    }

    public ApiException(String message, int statusCode, String responseBody) {
        this(message, statusCode, responseBody, null);
    }

    public ApiException(String message, Throwable cause) {
        this(message, 0, null, cause);
    }

    public ApiException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }

    public boolean isTimeout() {
        return statusCode == 0 && getCause() instanceof java.net.http.HttpTimeoutException;
    }

    public boolean isNetwork() {
        return statusCode == 0;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    public boolean isForbidden() {
        return statusCode == 403;
    }

    public boolean isLicenseRequired() {
        if (statusCode != 403) {
            return false;
        }
        String err = bodyField("error");
        if (err != null && "LICENSE_REQUIRED".equalsIgnoreCase(err)) {
            return true;
        }
        String msg = getMessage();
        return msg != null && msg.toLowerCase().contains("licence");
    }

    /** UUID a transmettre pour generer le .lic. */
    public String installationId() {
        return bodyField("installationId");
    }

    private String bodyField(String name) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = BODY_MAPPER.readTree(responseBody);
            if (node != null && node.hasNonNull(name)) {
                String v = node.get(name).asText("").trim();
                return v.isEmpty() ? null : v;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    public static String userMessage(ApiException e) {
        return userMessage(e, false);
    }

    public static String loginMessage(ApiException e) {
        return userMessage(e, true);
    }

    private static String userMessage(ApiException e, boolean login) {
        if (e.isTimeout()) {
            return "Le serveur ne répond pas (délai dépassé).";
        }
        if (e.isNetwork()) {
            return "Connexion au serveur impossible. Vérifiez que le serveur Gest POV est démarré.";
        }
        if (e.statusCode() == 401) {
            if (login) {
                return usableBackendMessage(e, "Email ou mot de passe incorrect.");
            }
            return "Votre session a expiré. Veuillez vous reconnecter.";
        }
        if (e.statusCode() == 403) {
            if (e.isLicenseRequired()) {
                String id = e.installationId();
                if (id != null) {
                    return "Licence requise. Identifiant serveur (server.id) : " + id;
                }
                return "Licence Gest POV requise. Ouvrez l'écran Licence pour copier le server.id.";
            }
            return "Vous n'avez pas l'autorisation d'effectuer cette opération.";
        }
        if (e.statusCode() == 404) {
            return usableBackendMessage(e, "Élément introuvable.");
        }
        if (e.statusCode() == 409) {
            return usableBackendMessage(e, "Cette action est impossible : un conflit a été détecté.");
        }
        if (e.statusCode() == 422) {
            return usableBackendMessage(e, "Certaines informations ne sont pas valides.");
        }
        if (e.statusCode() == 400) {
            return usableBackendMessage(e, "Les données envoyées sont invalides.");
        }
        if (e.statusCode() >= 500) {
            return "Une erreur technique est survenue. Réessayez dans quelques instants.";
        }
        return usableBackendMessage(e, "Une erreur est survenue.");
    }

    private static String usableBackendMessage(ApiException e, String fallback) {
        String raw = e.getMessage();
        if (raw == null || raw.isBlank() || isTechnical(raw)) {
            return fallback;
        }
        return raw;
    }

    private static boolean isTechnical(String message) {
        String lower = message.toLowerCase();
        return lower.contains("exception")
                || lower.contains("sql")
                || lower.startsWith("{")
                || lower.startsWith("erreur http")
                || lower.contains("nullpointer")
                || lower.contains("stack");
    }
}
