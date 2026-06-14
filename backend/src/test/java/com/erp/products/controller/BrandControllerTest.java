package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.dto.BrandRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BrandControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateAndListBrands() throws Exception {
        BrandRequest request = new BrandRequest();
        request.setNom("Adidas");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Adidas"));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldRejectDuplicateBrandName() throws Exception {
        BrandRequest request = new BrandRequest();
        request.setNom("Puma");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSearchBrands() throws Exception {
        BrandRequest request = new BrandRequest();
        request.setNom("Levi's");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/brands/search").param("nom", "Levi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Levi's"));
    }
}
