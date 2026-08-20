package com.erp.products.discovery;

import com.erp.products.config.DesktopProperties;
import com.erp.products.config.LicenseProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Identifiant serveur UUID persistant (indépendant de l'IP).
 * Fichier : app.desktop.server-id-file, sinon {license.data-dir}/server.id
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerIdentityService {

    public static final String DEFAULT_FILE_NAME = "server.id";

    private final DesktopProperties desktopProperties;
    private final LicenseProperties licenseProperties;

    private volatile String cachedId;

    @PostConstruct
    void init() {
        cachedId = loadOrCreate();
        log.info("serverId Gest POV : {} ({})", cachedId, resolveFile());
    }

    public String getServerId() {
        if (cachedId == null || cachedId.isBlank()) {
            cachedId = loadOrCreate();
        }
        return cachedId;
    }

    public Path resolveFile() {
        String configured = desktopProperties.getServerIdFile();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(licenseProperties.getDataDir()).toAbsolutePath().normalize().resolve(DEFAULT_FILE_NAME);
    }

    private synchronized String loadOrCreate() {
        try {
            Path path = resolveFile();
            Files.createDirectories(path.getParent());
            if (Files.isRegularFile(path)) {
                String existing = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!existing.isBlank()) {
                    return existing;
                }
            }
            String generated = UUID.randomUUID().toString();
            Files.writeString(path, generated, StandardCharsets.UTF_8);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire ou créer server.id : " + e.getMessage(), e);
        }
    }
}
