package com.gestpov.desktop.session;

import com.gestpov.desktop.net.ApiClient;
import com.gestpov.desktop.net.AuthSession;

import java.util.List;

/**
 * Session Desktop centralisée. Le JWT reste dans {@link ApiClient} (mémoire uniquement).
 */
public final class SessionContext {

    private final ApiClient api;
    private AuthSession user;
    private String serverId = "";
    private String serverName = "";
    private String serverVersion = "";
    private Runnable sessionExpiredHandler;

    public SessionContext(ApiClient api) {
        this.api = api;
        this.api.setUnauthorizedHandler(ignored -> {
            clear();
            Runnable handler = sessionExpiredHandler;
            if (handler != null) {
                handler.run();
            }
        });
    }

    public ApiClient api() {
        return api;
    }

    public void onSessionExpired(Runnable handler) {
        this.sessionExpiredHandler = handler;
    }

    public void bindServer(String serverId, String serverName, String serverVersion) {
        this.serverId = serverId == null ? "" : serverId;
        this.serverName = serverName == null ? "" : serverName;
        this.serverVersion = serverVersion == null ? "" : serverVersion;
    }

    public void setUser(AuthSession user) {
        this.user = user;
    }

    public AuthSession user() {
        return user;
    }

    public boolean isAuthenticated() {
        return user != null && api.getBearerToken() != null && !api.getBearerToken().isBlank();
    }

    public boolean hasPermission(String permission) {
        if (user == null || permission == null) {
            return false;
        }
        List<String> permissions = user.permissions();
        return permissions != null && permissions.contains(permission);
    }

    public String serverId() {
        return serverId;
    }

    public String serverName() {
        return serverName;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public String displayName() {
        return user == null ? "" : user.displayName();
    }

    public String email() {
        return user == null ? "" : user.email();
    }

    public void clear() {
        api.clearToken();
        user = null;
    }
}
