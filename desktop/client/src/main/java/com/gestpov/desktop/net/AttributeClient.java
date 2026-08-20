package com.gestpov.desktop.net;

import com.gestpov.desktop.model.CustomAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AttributeClient {

    private final ApiClient api;

    public AttributeClient(ApiClient api) {
        this.api = api;
    }

    public List<CustomAttribute> findAll() throws ApiException {
        return JsonLists.mapArray(api.get("/api/attributes"), CustomAttribute::fromJson);
    }

    public CustomAttribute create(String code, String label, String type) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("label", label);
        body.put("type", type);
        return CustomAttribute.fromJson(api.post("/api/attributes", body));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/attributes/" + id);
    }
}
