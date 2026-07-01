package com.erp.products.license;

import com.erp.products.config.LicenseProperties;
import com.erp.products.repository.UserRepository;
import com.erp.products.support.LicenseTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LicenseServiceTest {

    @TempDir
    Path tempDir;

    private Path privateKeyPath;
    private LicenseService licenseService;
    private String installationId;

    @BeforeEach
    void setUp() throws Exception {
        privateKeyPath = Path.of("src/test/resources/keys/test_private_key.pem");
        LicenseProperties properties = new LicenseProperties();
        properties.setDataDir(tempDir.toString());
        properties.setEnforcementEnabled(true);
        properties.setPublicKeyResource("classpath:keys/test_public_key.pem");

        LicenseSignatureVerifier verifier = new LicenseSignatureVerifier(
                properties, new DefaultResourceLoader());
        licenseService = new LicenseService(
                properties,
                verifier,
                new ObjectMapper(),
                mock(UserRepository.class));
        licenseService.init();
        installationId = licenseService.getInstallationId();
    }

    @Test
    void shouldReportMissingLicense() {
        LicenseStatusResponse status = licenseService.getStatus();
        assertThat(status.isValid()).isFalse();
        assertThat(status.getReason()).isEqualTo(LicenseInvalidReason.LICENSE_MISSING.name());
        assertThat(status.getInstallationId()).isEqualTo(installationId);
        assertThat(status.isActivated()).isFalse();
    }

    @Test
    void shouldAcceptValidLicense() throws Exception {
        String content = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload(installationId), privateKeyPath);
        Files.writeString(tempDir.resolve("gest_pov.lic"), content);
        licenseService.refreshStatus();

        LicenseStatusResponse status = licenseService.getStatus();
        assertThat(status.isValid()).isTrue();
        assertThat(status.isActivated()).isTrue();
        assertThat(status.getLicenseId()).isEqualTo("GPV-TEST-001");
        assertThat(status.getClient()).isEqualTo("Client Test");
        assertThat(status.getDaysRemaining()).isPositive();
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        String content = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload(installationId), privateKeyPath);
        content = content.replaceFirst(
                "\"signature\"\\s*:\\s*\"[^\"]+\"",
                "\"signature\":\"invalid-signature-base64==\"");
        Files.writeString(tempDir.resolve("gest_pov.lic"), content);
        licenseService.refreshStatus();

        assertThat(licenseService.getStatus().getReason()).isEqualTo(LicenseInvalidReason.INVALID_SIGNATURE.name());
    }

    @Test
    void shouldRejectInstallationMismatch() throws Exception {
        String content = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload("wrong-installation-id"), privateKeyPath);
        Files.writeString(tempDir.resolve("gest_pov.lic"), content);
        licenseService.refreshStatus();

        assertThat(licenseService.getStatus().getReason())
                .isEqualTo(LicenseInvalidReason.INSTALLATION_MISMATCH.name());
    }

    @Test
    void shouldRejectExpiredLicense() throws Exception {
        var payload = LicenseTestSupport.validPayload(installationId);
        payload.setExpiresAt("2020-01-01");
        String content = LicenseTestSupport.buildSignedLicenseFile(payload, privateKeyPath);
        Files.writeString(tempDir.resolve("gest_pov.lic"), content);
        licenseService.refreshStatus();

        assertThat(licenseService.getStatus().getReason()).isEqualTo(LicenseInvalidReason.EXPIRED.name());
    }

    @Test
    void shouldImportValidLicenseFile() throws Exception {
        String content = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload(installationId), privateKeyPath);
        MockMultipartFile file = new MockMultipartFile(
                "file", "gest_pov.lic", "application/json", content.getBytes(StandardCharsets.UTF_8));

        LicenseStatusResponse imported = licenseService.importLicense(file);
        assertThat(imported.isValid()).isTrue();
        assertThat(Files.exists(tempDir.resolve("gest_pov.lic"))).isTrue();
    }

    @Test
    void shouldEnforceMaxUsers() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.countByIsActiveTrue()).thenReturn(10L);

        LicenseProperties properties = new LicenseProperties();
        properties.setDataDir(tempDir.toString());
        properties.setPublicKeyResource("classpath:keys/test_public_key.pem");
        properties.setEnforcementEnabled(true);
        LicenseSignatureVerifier verifier = new LicenseSignatureVerifier(
                properties, new DefaultResourceLoader());
        LicenseService service = new LicenseService(
                properties, verifier, new ObjectMapper(), userRepository);
        service.init();

        String content = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload(service.getInstallationId()), privateKeyPath);
        Files.writeString(tempDir.resolve("gest_pov.lic"), content);
        service.refreshStatus();

        assertThatThrownBy(service::assertCanCreateUser)
                .hasMessageContaining("Limite de licence");
    }
}
