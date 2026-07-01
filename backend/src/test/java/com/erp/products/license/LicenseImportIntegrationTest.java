package com.erp.products.license;

import org.junit.jupiter.api.Test;import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.license.enforcement-enabled=true")
class LicenseImportIntegrationTest extends com.erp.products.AbstractIntegrationTest {

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
}
