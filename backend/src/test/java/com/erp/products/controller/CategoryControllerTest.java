package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.dto.CategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateAndGetCategoryTree() throws Exception {
        CategoryRequest parent = new CategoryRequest();
        parent.setNom("Électronique");

        String parentResponse = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Électronique"))
                .andReturn().getResponse().getContentAsString();

        Long parentId = objectMapper.readTree(parentResponse).get("id").asLong();

        CategoryRequest child = new CategoryRequest();
        child.setNom("Téléphones");
        child.setParentId(parentId);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parentId));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].nom").value("Téléphones"));
    }

    @Test
    void shouldSearchCategories() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setNom("Vêtements");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/categories/search").param("nom", "Vêt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Vêtements"));
    }

    @Test
    void shouldUpdateAndDeleteCategory() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setNom("Sport");

        String response = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        request.setNom("Sport & Loisirs");
        mockMvc.perform(put("/api/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Sport & Loisirs"));

        mockMvc.perform(delete("/api/categories/{id}", id))
                .andExpect(status().isNoContent());
    }
}
