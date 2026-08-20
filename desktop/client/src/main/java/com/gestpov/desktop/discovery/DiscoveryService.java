package com.gestpov.desktop.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.config.ClientConfig;
import com.gestpov.desktop.net.ApiClient;
import com.gestpov.desktop.net.ApiException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Ordre : serveur mémorisé → hostname → UDP → validation HTTP obligatoire.
 */
public class DiscoveryService {

    private final UdpDiscoveryClient udp;
    private final BiFunction<String, Integer, ApiClient> apiFactory;
    private final String clientVersion;

    public DiscoveryService(UdpDiscoveryClient udp, Duration httpTimeout, String clientVersion) {
        this(udp, (host, port) -> new ApiClient(host, port, httpTimeout), clientVersion);
    }

    public DiscoveryService(
            UdpDiscoveryClient udp,
            BiFunction<String, Integer, ApiClient> apiFactory,
            String clientVersion) {
        this.udp = udp;
        this.apiFactory = apiFactory;
        this.clientVersion = clientVersion;
    }

    public List<DiscoveredServer> findServers(ClientConfig config) {
        Map<String, DiscoveredServer> servers = new LinkedHashMap<>();

        if (config.hasRememberedServer()) {
            addIfValid(servers, config.host(), config.port());
        }
        if (config.hostnameFallback() != null && !config.hostnameFallback().isBlank()) {
            addIfValid(servers, config.hostnameFallback().trim(), config.port() > 0 ? config.port() : 8080);
        }

        if (udp != null) {
            for (UdpDiscoveryClient.UdpCandidate candidate : udp.probe(config.discoveryUdpPort())) {
                addIfValid(servers, candidate.host(), candidate.httpPort());
            }
        }

        if (config.serverId() != null && !config.serverId().isBlank()) {
            for (DiscoveredServer server : new ArrayList<>(servers.values())) {
                if (config.serverId().equals(server.serverId()) && !config.host().equals(server.host())) {
                    // même serverId, IP changée : déjà dans la map
                    break;
                }
            }
        }
        return new ArrayList<>(servers.values());
    }

    public DiscoveredServer validateHttp(String host, int port) throws ApiException {
        ApiClient client = apiFactory.apply(host, port);
        JsonNode json = client.get("/api/discovery");
        DiscoveredServer server = DiscoveredServer.fromHttp(host, port, json, clientVersion);
        if (!server.isGestPov()) {
            throw new ApiException("Ce serveur n'est pas Gest POV");
        }
        return server;
    }

    private void addIfValid(Map<String, DiscoveredServer> servers, String host, int port) {
        try {
            DiscoveredServer server = validateHttp(host, port);
            servers.put(server.serverId() + "@" + host + ":" + server.port(), server);
        } catch (Exception ignored) {
            // candidat rejeté
        }
    }
}
