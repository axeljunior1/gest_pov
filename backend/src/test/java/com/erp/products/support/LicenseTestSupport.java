package com.erp.products.support;

import com.erp.products.license.LicensePayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Génère des fichiers .lic signés pour les tests (clé privée uniquement en test).
 */
public final class LicenseTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LicenseTestSupport() {
    }

    public static String buildSignedLicenseFile(LicensePayload payload, Path privateKeyPem) throws Exception {
        String json = MAPPER.writeValueAsString(payload);
        String payloadBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = signPayload(payloadBase64, privateKeyPem);
        return MAPPER.writeValueAsString(java.util.Map.of(
                "payload", payloadBase64,
                "signature", signatureBase64));
    }

    public static String signPayload(String payloadBase64, Path privateKeyPem) throws Exception {
        PrivateKey privateKey = loadPrivateKey(privateKeyPem);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payloadBase64.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static PrivateKey loadPrivateKey(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath, StandardCharsets.UTF_8)
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    public static LicensePayload validPayload(String installationId) {
        LicensePayload payload = new LicensePayload();
        payload.setLicenseId("GPV-TEST-001");
        payload.setClient("Client Test");
        payload.setSite("Site Test");
        payload.setInstallationId(installationId);
        payload.setMaxUsers(10);
        payload.setIssuedAt("2026-01-01");
        payload.setExpiresAt("2099-12-31");
        payload.setApp("gest_pov");
        return payload;
    }
}
