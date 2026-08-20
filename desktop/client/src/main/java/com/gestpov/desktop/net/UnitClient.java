package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Unit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnitClient {

    private final ApiClient api;

    public UnitClient(ApiClient api) {
        this.api = api;
    }

    public List<Unit> findAll() throws ApiException {
        return JsonLists.mapArray(api.get("/api/units"), Unit::fromJson);
    }

    public Unit create(String nom, String symbole) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom", nom);
        body.put("symbole", symbole);
        return Unit.fromJson(api.post("/api/units", body));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/units/" + id);
    }
}
