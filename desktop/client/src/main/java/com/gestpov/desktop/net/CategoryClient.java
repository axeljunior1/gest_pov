package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Category;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accès REST Catégories — mapping DTO uniquement. Parent, cycles et produits restent côté API.
 */
public class CategoryClient {

    private final ApiClient api;

    public CategoryClient(ApiClient api) {
        this.api = api;
    }

    public List<Category> getTree() throws ApiException {
        return parseList(api.get("/api/categories"));
    }

    public List<Category> search(String nom) throws ApiException {
        return parseList(api.get("/api/categories/search", Map.of("nom", nom == null ? "" : nom)));
    }

    public Category create(String nom, Long parentId) throws ApiException {
        return Category.fromJson(api.post("/api/categories", body(nom, parentId)));
    }

    public Category update(long id, String nom, Long parentId) throws ApiException {
        return Category.fromJson(api.put("/api/categories/" + id, body(nom, parentId)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/categories/" + id);
    }

    private static Map<String, Object> body(String nom, Long parentId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nom", nom);
        if (parentId != null) {
            payload.put("parentId", parentId);
        }
        return payload;
    }

    static List<Category> parseList(JsonNode node) {
        List<Category> categories = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return categories;
        }
        node.forEach(item -> {
            Category category = Category.fromJson(item);
            if (category != null) {
                categories.add(category);
            }
        });
        return categories;
    }
}
