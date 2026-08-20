package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.Supplier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupplierClient {

    private final ApiClient api;

    public SupplierClient(ApiClient api) {
        this.api = api;
    }

    public List<Supplier> findAll() throws ApiException {
        return JsonLists.mapArray(api.get("/api/suppliers"), Supplier::fromJson);
    }

    public List<Supplier> search(String nom) throws ApiException {
        return JsonLists.mapArray(api.get("/api/suppliers/search", Map.of("nom", nom == null ? "" : nom)),
                Supplier::fromJson);
    }

    public Supplier create(Supplier supplier) throws ApiException {
        return Supplier.fromJson(api.post("/api/suppliers", body(supplier)));
    }

    public Supplier update(long id, Supplier supplier) throws ApiException {
        return Supplier.fromJson(api.put("/api/suppliers/" + id, body(supplier)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/suppliers/" + id);
    }

    private static Map<String, Object> body(Supplier supplier) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom", supplier.nom());
        if (supplier.email() != null && !supplier.email().isBlank()) {
            body.put("email", supplier.email());
        }
        if (supplier.telephone() != null && !supplier.telephone().isBlank()) {
            body.put("telephone", supplier.telephone());
        }
        if (supplier.adresse() != null && !supplier.adresse().isBlank()) {
            body.put("adresse", supplier.adresse());
        }
        return body;
    }
}
