package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Brand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Accès REST Marques — aucun métier local au-delà du mapping DTO.
 */
public class BrandClient {

    private final ApiClient api;

    public BrandClient(ApiClient api) {
        this.api = api;
    }

    public List<Brand> findAll() throws ApiException {
        return parseList(api.get("/api/brands"));
    }

    public List<Brand> search(String nom) throws ApiException {
        return parseList(api.get("/api/brands/search", Map.of("nom", nom == null ? "" : nom)));
    }

    public Brand create(String nom) throws ApiException {
        return Brand.fromJson(api.post("/api/brands", Map.of("nom", nom)));
    }

    public Brand update(long id, String nom) throws ApiException {
        return Brand.fromJson(api.put("/api/brands/" + id, Map.of("nom", nom)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/brands/" + id);
    }

    static List<Brand> parseList(JsonNode node) {
        List<Brand> brands = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return brands;
        }
        node.forEach(item -> {
            Brand brand = Brand.fromJson(item);
            if (brand != null) {
                brands.add(brand);
            }
        });
        return brands;
    }
}
