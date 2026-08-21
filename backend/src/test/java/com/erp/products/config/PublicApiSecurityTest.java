package com.erp.products.config;

import com.erp.products.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.security.use-prod-chain=true")
class PublicApiSecurityTest extends AbstractIntegrationTest {

    @Test
    void discovery_isPublic_noJwt() throws Exception {
        mockMvc.perform(get("/api/discovery").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("GEST_POV"))
                .andExpect(jsonPath("$.serverId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void login_isPublic_badPayloadIs400Not403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brands_withoutJwt_is401NotEmpty403() throws Exception {
        mockMvc.perform(get("/api/brands").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification requise"));
    }

    @Test
    void unknownApiPath_withJwt_is404Not401() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/import/templates/does-not-exist")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private String loginToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TestAuthReferenceDataInitializer.ADMIN_EMAIL,
                                "password", TestAuthReferenceDataInitializer.ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }
}
