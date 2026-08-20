package com.gestpov.desktop.config;

/**
 * Configuration client Desktop — jamais de secrets PostgreSQL ni mot de passe utilisateur.
 */
public record ClientConfig(
        String serverId,
        String host,
        int port,
        String serverName,
        String hostnameFallback,
        String clientVersion,
        int discoveryUdpPort,
        int timeoutMs
) {
    public static final String CURRENT_VERSION = "1.0.0";

    public static ClientConfig empty() {
        return new ClientConfig("", "", 8080, "", "", CURRENT_VERSION, 38471, 20000);
    }

    public ClientConfig withServer(String id, String newHost, int newPort, String name) {
        return new ClientConfig(
                id == null ? "" : id,
                newHost == null ? "" : newHost,
                newPort,
                name == null ? "" : name,
                hostnameFallback,
                clientVersion,
                discoveryUdpPort,
                timeoutMs
        );
    }

    public boolean hasRememberedServer() {
        return host != null && !host.isBlank() && port > 0;
    }
}
