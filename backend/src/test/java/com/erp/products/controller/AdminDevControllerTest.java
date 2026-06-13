package com.erp.products.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class AdminDevControllerTest extends com.erp.products.AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
