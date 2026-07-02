package com.erp.products.license;

import com.erp.products.support.LicenseTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.license.enforcement-enabled=true",
        "app.license.public-key-resource=classpath:keys/test_public_key.pem",
        "app.license.data-dir=target/test-license-integration"
})
class LicenseImportIntegrationTest extends com.erp.products.AbstractIntegrationTest {

    private static final Path TEST_PRIVATE_KEY = Path.of("src/test/resources/keys/test_private_key.pem");
    private static final Path LICENSE_DATA_DIR = Path.of("target/test-license-integration");

    @Autowired
    private LicenseService licenseService;

    @BeforeEach
    void resetLicenseFile() throws Exception {
        Files.createDirectories(LICENSE_DATA_DIR);
        Files.deleteIfExists(LICENSE_DATA_DIR.resolve("gest_pov.lic"));
        licenseService.refreshStatus();
    }

    @Test
    void shouldAllowLicenseEndpointsWithoutValidLicense() throws Exception {
        mockMvc.perform(get("/api/license/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        mockMvc.perform(get("/api/license/installation-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationId").exists());
    }

    @Test
    void shouldNotReturn403OnLicenseImportWithoutValidLicense() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gest_pov.lic",
                MediaType.TEXT_PLAIN_VALUE,
                "{}".getBytes());

        mockMvc.perform(multipart("/api/license/import").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldBlockBusinessApiWhenLicenseMissing() throws Exception {
        String token = loginToken("admin@erp.local", "ErpAdmin2026!");

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("LICENSE_REQUIRED")));
    }

    @Test
    void shouldUnblockBusinessApiAfterValidLicenseImport() throws Exception {
        MvcResult installationResult = mockMvc.perform(get("/api/license/installation-id"))
                .andExpect(status().isOk())
                .andReturn();
        String installationId = objectMapper.readTree(installationResult.getResponse().getContentAsString())
                .get("installationId").asText();

        String licenseContent = LicenseTestSupport.buildSignedLicenseFile(
                LicenseTestSupport.validPayload(installationId), TEST_PRIVATE_KEY);
        MockMultipartFile file = new MockMultipartFile(
                "file", "gest_pov.lic", MediaType.APPLICATION_JSON_VALUE,
                licenseContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/license/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));

        String token = loginToken("admin@erp.local", "ErpAdmin2026!");

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
