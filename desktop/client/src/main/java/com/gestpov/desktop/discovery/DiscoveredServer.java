package com.gestpov.desktop.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.version.CompatibilityStatus;
import com.gestpov.desktop.version.VersionCompatibility;

public record DiscoveredServer(
        String application,
        String serverId,
        String serverName,
        String version,
        String status,
        String companyName,
        String host,
        int port,
        CompatibilityStatus compatibility
) {
    public static DiscoveredServer fromHttp(String host, int port, JsonNode json, String clientVersion) {
        String app = json.path("application").asText("");
        String serverVersion = json.path("version").asText("");
        return new DiscoveredServer(
                app,
                json.path("serverId").asText(""),
                json.path("serverName").asText(""),
                serverVersion,
                json.path("status").asText(""),
                json.path("companyName").asText(null),
                host,
                json.has("port") ? json.path("port").asInt(port) : port,
                VersionCompatibility.compare(clientVersion, serverVersion)
        );
    }

    public boolean isGestPov() {
        return "GEST_POV".equals(application);
    }

    public String label() {
        String name = (serverName == null || serverName.isBlank()) ? host : serverName;
        String company = (companyName == null || companyName.isBlank()) ? "" : " — " + companyName;
        return name + " (" + host + ":" + port + ")" + company;
    }
}
