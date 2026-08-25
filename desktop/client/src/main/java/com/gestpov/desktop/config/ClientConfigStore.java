package com.gestpov.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistance %APPDATA%\\GestPOV\\client.properties (prod)
 * ou chemin injecté (tests / dev).
 */
public class ClientConfigStore {

    private final Path configFile;

    public ClientConfigStore(Path configFile) {
        this.configFile = configFile;
    }

    public static ClientConfigStore userDefault() {
        Path user = Path.of(System.getenv().getOrDefault("APPDATA", "."), "GestPOV", "client.properties");
        Path dev = Path.of("config", "client.properties");
        if (Files.isRegularFile(dev)) {
            return new ClientConfigStore(dev);
        }
        return new ClientConfigStore(user);
    }

    public Path path() {
        return configFile;
    }

    public ClientConfig load() {
        Properties props = new Properties();
        if (Files.isRegularFile(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
            } catch (IOException ignored) {
                return ClientConfig.empty();
            }
        }
        return new ClientConfig(
                props.getProperty("server.id", ""),
                props.getProperty("server.host", ""),
                parseInt(props.getProperty("server.port"), 8080),
                props.getProperty("server.name", ""),
                props.getProperty("server.hostname", ""),
                props.getProperty("client.version", ClientConfig.CURRENT_VERSION),
                parseInt(props.getProperty("discovery.udp.port"), 38471),
                parseInt(props.getProperty("http.timeout.ms"), 20000),
                props.getProperty("auth.last.email", ""),
                props.getProperty("ticket.printer.name", ""),
                parseInt(props.getProperty("ticket.printer.width.chars"), ClientConfig.DEFAULT_TICKET_WIDTH_CHARS)
        );
    }

    public void save(ClientConfig config) throws IOException {
        Files.createDirectories(configFile.getParent() == null ? Path.of(".") : configFile.getParent());
        Properties props = new Properties();
        props.setProperty("server.id", nullToEmpty(config.serverId()));
        props.setProperty("server.host", nullToEmpty(config.host()));
        props.setProperty("server.port", String.valueOf(config.port()));
        props.setProperty("server.name", nullToEmpty(config.serverName()));
        props.setProperty("server.hostname", nullToEmpty(config.hostnameFallback()));
        props.setProperty("client.version", nullToEmpty(config.clientVersion()));
        props.setProperty("discovery.udp.port", String.valueOf(config.discoveryUdpPort()));
        props.setProperty("http.timeout.ms", String.valueOf(config.timeoutMs()));
        props.setProperty("auth.last.email", nullToEmpty(config.lastLoginEmail()));
        props.setProperty("ticket.printer.name", nullToEmpty(config.ticketPrinterName()));
        props.setProperty("ticket.printer.width.chars", String.valueOf(config.ticketPaperWidthChars()));
        try (OutputStream out = Files.newOutputStream(configFile)) {
            props.store(out, "Gest POV Desktop — pas de secrets DB");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static int parseInt(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
