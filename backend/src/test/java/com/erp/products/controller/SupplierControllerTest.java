package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.dto.SupplierRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SupplierControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateAndListSuppliers() throws Exception {
        SupplierRequest request = new SupplierRequest();
        request.setNom("TechSupply");
        request.setEmail("contact@techsupply.com");
        request.setTelephone("0123456789");

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("TechSupply"))
                .andExpect(jsonPath("$.email").value("contact@techsupply.com"));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldSearchSuppliers() throws Exception {
        SupplierRequest request = new SupplierRequest();
        request.setNom("Nike Distribution");

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/suppliers/search").param("nom", "Distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Nike Distribution"));
    }
}
