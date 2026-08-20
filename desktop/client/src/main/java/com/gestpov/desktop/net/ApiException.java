package com.gestpov.desktop.net;

public class ApiException extends Exception {

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

    /**
     * Message UI. Jamais de stack, SQL ni JSON brut.
     */
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
            String backend = e.getMessage();
            if (backend != null && backend.toLowerCase().contains("licence")) {
                return backend;
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
