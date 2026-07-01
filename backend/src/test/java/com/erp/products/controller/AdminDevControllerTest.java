package com.erp.products.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminDevController est {@code @Profile("dev")} ; ce test active {@code dev} tout en
 * conservant la datasource H2 du profil {@code test} (évite le conflit PostgreSQL).
 */
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admdevtest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AdminDevControllerTest extends com.erp.products.AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeDevToolsStatus() throws Exception {
        String token = loginToken("admin@erp.local", "ErpAdmin2026!");
        mockMvc.perform(get("/api/admin/dev-tools/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile", is("dev")))
                .andExpect(jsonPath("$.resetEnabled", is(true)));
    }

    @Test
    void shouldResetAndSeedDemoWithAdminToken() throws Exception {
        jdbcTemplate.update("INSERT INTO categories (nom, created_at, updated_at) VALUES ('Temp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        String token = loginToken("admin@erp.local", "ErpAdmin2026!");

        mockMvc.perform(post("/api/admin/reset-demo")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Reset-Token", "dev-reset-token-change-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OK")));

        mockMvc.perform(post("/api/admin/seed-demo")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Reset-Token", "dev-reset-token-change-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.productSku", is("DEMO-EAU-1L")));
    }

    @Test
    void shouldRejectInvalidResetToken() throws Exception {
        String token = loginToken("admin@erp.local", "ErpAdmin2026!");
        mockMvc.perform(post("/api/admin/reset-demo")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Reset-Token", "wrong"))
                .andExpect(status().isBadRequest());
    }

    private String loginToken(String email, String password) throws Exception {
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }
}
