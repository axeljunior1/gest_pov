package com.erp.products.license;

import com.erp.products.config.LicenseProperties;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseService {

    private final LicenseProperties properties;
    private final LicenseSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private volatile LicenseStatusResponse cachedStatus;

    @PostConstruct
    void init() {
        try {
            ensureDataDirectory();
            ensureInstallationId();
            refreshStatus();
            if (!isLicenseValid()) {
                log.warn("Gest_POV demarre sans licence valide: {}",
                        cachedStatus != null ? cachedStatus.getReason() : LicenseInvalidReason.LICENSE_MISSING.name());
            } else {
                log.info("Licence active: {} — expire le {}", cachedStatus.getLicenseId(), cachedStatus.getExpiresAt());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'initialiser le dossier licence: " + e.getMessage(), e);
        }
    }

    public LicenseStatusResponse getStatus() {
        if (cachedStatus == null) {
            refreshStatus();
        }
        return cachedStatus;
    }

    public boolean isLicenseValid() {
        LicenseStatusResponse status = getStatus();
        return status != null && status.isValid();
    }

    public boolean isEnforcementEnabled() {
        return properties.isEnforcementEnabled();
    }

    public String getInstallationId() {
        try {
            return ensureInstallationId();
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire l'identifiant d'installation");
        }
    }

    public LicenseStatusResponse importLicense(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fichier licence vide");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        LicenseValidationResult result = validateLicenseContent(content);
        if (!result.valid()) {
            throw new BusinessException(mapReasonMessage(result.reason()));
        }
        Files.writeString(licenseFilePath(), content, StandardCharsets.UTF_8);
        refreshStatus();
        return cachedStatus;
    }

    public void assertCanCreateUser() {
        if (!isLicenseValid()) {
            throw new BusinessException("Licence invalide ou absente — creation utilisateur impossible");
        }
        LicensePayload payload = loadPayloadFromFile();
        if (payload == null || payload.getMaxUsers() == null) {
            return;
        }
        long activeUsers = userRepository.countByIsActiveTrue();
        if (activeUsers >= payload.getMaxUsers()) {
            throw new BusinessException(
                    "Limite de licence atteinte (" + payload.getMaxUsers() + " utilisateurs actifs max)");
        }
    }

    public synchronized void refreshStatus() {
        cachedStatus = buildStatus();
    }

    private LicenseStatusResponse buildStatus() {
        String installationId;
        try {
            installationId = ensureInstallationId();
        } catch (IOException e) {
            return invalidStatus(LicenseInvalidReason.LICENSE_MISSING, null);
        }

        if (!Files.isRegularFile(licenseFilePath())) {
            return LicenseStatusResponse.builder()
                    .valid(false)
                    .activated(false)
                    .reason(LicenseInvalidReason.LICENSE_MISSING.name())
                    .installationId(installationId)
                    .build();
        }

        try {
            String content = Files.readString(licenseFilePath(), StandardCharsets.UTF_8);
            LicenseValidationResult result = validateLicenseContent(content);
            if (!result.valid()) {
                return LicenseStatusResponse.builder()
                        .valid(false)
                        .activated(false)
                        .reason(result.reason().name())
                        .installationId(installationId)
                        .build();
            }
            LicensePayload payload = result.payload();
            LocalDate expiresAt = parseDate(payload.getExpiresAt());
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiresAt);
            return LicenseStatusResponse.builder()
                    .valid(true)
                    .activated(true)
                    .licenseId(payload.getLicenseId())
                    .client(payload.getClient())
                    .site(payload.getSite())
                    .issuedAt(payload.getIssuedAt())
                    .expiresAt(payload.getExpiresAt())
                    .daysRemaining(daysRemaining)
                    .maxUsers(payload.getMaxUsers())
                    .installationId(installationId)
                    .build();
        } catch (Exception e) {
            log.debug("Erreur lecture licence: {}", e.getMessage());
            return LicenseStatusResponse.builder()
                    .valid(false)
                    .activated(false)
                    .reason(LicenseInvalidReason.INVALID_FORMAT.name())
                    .installationId(installationId)
                    .build();
        }
    }

    LicenseValidationResult validateLicenseContent(String content) {
        String installationId;
        try {
            installationId = ensureInstallationId();
        } catch (IOException e) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.LICENSE_MISSING);
        }

        LicenseFileEnvelope envelope;
        try {
            envelope = objectMapper.readValue(content, LicenseFileEnvelope.class);
        } catch (Exception e) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INVALID_FORMAT);
        }
        if (envelope.getPayload() == null || envelope.getSignature() == null) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INVALID_FORMAT);
        }
        if (!signatureVerifier.verify(envelope.getPayload(), envelope.getSignature())) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INVALID_SIGNATURE);
        }

        LicensePayload payload;
        try {
            byte[] jsonBytes = Base64.getDecoder().decode(envelope.getPayload());
            payload = objectMapper.readValue(jsonBytes, LicensePayload.class);
        } catch (Exception e) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INVALID_FORMAT);
        }

        if (payload.getInstallationId() == null
                || !installationId.equalsIgnoreCase(payload.getInstallationId().trim())) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INSTALLATION_MISMATCH);
        }

        if (payload.getApp() != null && !payload.getApp().isBlank()
                && !properties.getExpectedApp().equalsIgnoreCase(payload.getApp().trim())) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.INVALID_APP);
        }

        LocalDate expiresAt = parseDate(payload.getExpiresAt());
        if (expiresAt.isBefore(LocalDate.now())) {
            return LicenseValidationResult.invalid(LicenseInvalidReason.EXPIRED);
        }

        return LicenseValidationResult.valid(payload);
    }

    private LicensePayload loadPayloadFromFile() {
        if (!Files.isRegularFile(licenseFilePath())) {
            return null;
        }
        try {
            String content = Files.readString(licenseFilePath(), StandardCharsets.UTF_8);
            LicenseValidationResult result = validateLicenseContent(content);
            return result.valid() ? result.payload() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Date licence manquante");
        }
        return LocalDate.parse(value.trim());
    }

    private LicenseStatusResponse invalidStatus(LicenseInvalidReason reason, String installationId) {
        return LicenseStatusResponse.builder()
                .valid(false)
                .activated(false)
                .reason(reason.name())
                .installationId(installationId)
                .build();
    }

    private String mapReasonMessage(LicenseInvalidReason reason) {
        return switch (reason) {
            case LICENSE_MISSING -> "Aucun fichier licence";
            case INVALID_FORMAT -> "Format de licence invalide";
            case INVALID_SIGNATURE -> "Signature RSA invalide";
            case INSTALLATION_MISMATCH -> "Licence non valide pour cette installation (installationId different)";
            case EXPIRED -> "Licence expiree";
            case INVALID_APP -> "Licence non destinee a Gest_POV";
        };
    }

    private void ensureDataDirectory() throws IOException {
        Files.createDirectories(dataDirectory());
    }

    private String ensureInstallationId() throws IOException {
        Path path = installationIdPath();
        if (Files.isRegularFile(path)) {
            String id = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (!id.isBlank()) {
                return id;
            }
        }
        String generated = UUID.randomUUID().toString();
        Files.writeString(path, generated, StandardCharsets.UTF_8);
        return generated;
    }

    private Path dataDirectory() {
        return Path.of(properties.getDataDir()).toAbsolutePath().normalize();
    }

    private Path installationIdPath() {
        return dataDirectory().resolve(properties.getInstallationIdFileName());
    }

    private Path licenseFilePath() {
        return dataDirectory().resolve(properties.getLicenseFileName());
    }

    record LicenseValidationResult(boolean valid, LicenseInvalidReason reason, LicensePayload payload) {
        static LicenseValidationResult valid(LicensePayload payload) {
            return new LicenseValidationResult(true, null, payload);
        }

        static LicenseValidationResult invalid(LicenseInvalidReason reason) {
            return new LicenseValidationResult(false, reason, null);
        }
    }
}
