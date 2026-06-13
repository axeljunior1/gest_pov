package com.erp.products.controller;

import com.erp.products.config.TestAuthReferenceDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PosCashierAccessTest extends com.erp.products.AbstractIntegrationTest {

    @Test
    void cashierShouldAccessPosButNotProductsApi() throws Exception {
        String token = loginToken(
                TestAuthReferenceDataInitializer.CASHIER_EMAIL,
                TestAuthReferenceDataInitializer.CASHIER_PASSWORD);

        mockMvc.perform(get("/api/pos/context")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
