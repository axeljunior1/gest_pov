package com.erp.products.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends com.erp.products.AbstractIntegrationTest {

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@erp.local",
                                "password", "ErpAdmin2026!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("admin@erp.local")))
                .andExpect(jsonPath("$.permissions", not(empty())));
    }

    @Test
    void login_withInvalidPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@erp.local",
                                "password", "wrong"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("incorrect")));
    }

    @Test
    void me_withoutToken_returns400() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Non authentifie")));
    }

    @Test
    void me_withValidToken_returnsUser() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@erp.local",
                                "password", "ErpAdmin2026!"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@erp.local")));
    }
}
