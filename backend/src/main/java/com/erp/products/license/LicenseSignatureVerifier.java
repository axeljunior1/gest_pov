package com.erp.products.license;

import com.erp.products.config.LicenseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class LicenseSignatureVerifier {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final LicenseProperties properties;
    private final ResourceLoader resourceLoader;

    private volatile PublicKey publicKey;

    public boolean verify(String payloadBase64, String signatureBase64) {
        if (payloadBase64 == null || payloadBase64.isBlank()
                || signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(loadPublicKey());
            signature.update(payloadBase64.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private PublicKey loadPublicKey() throws Exception {
        if (publicKey != null) {
            return publicKey;
        }
        synchronized (this) {
            if (publicKey != null) {
                return publicKey;
            }
            Resource resource = resourceLoader.getResource(properties.getPublicKeyResource());
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String normalized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
            return publicKey;
        }
    }
}
