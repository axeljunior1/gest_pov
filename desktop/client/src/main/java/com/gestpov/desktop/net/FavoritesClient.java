package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ecrans favoris de l'utilisateur connecte (barre laterale) — lies au compte. */
public class FavoritesClient {

    private final ApiClient api;

    public FavoritesClient(ApiClient api) {
        this.api = api;
    }

    public List<String> list() throws ApiException {
        JsonNode node = api.get("/api/users/me/favorites");
        List<String> keys = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> keys.add(n.asText()));
        }
        return keys;
    }

    public void add(String navItemKey) throws ApiException {
        api.post("/api/users/me/favorites/" + navItemKey, Map.of());
    }

    public void remove(String navItemKey) throws ApiException {
        api.delete("/api/users/me/favorites/" + navItemKey);
    }
}
