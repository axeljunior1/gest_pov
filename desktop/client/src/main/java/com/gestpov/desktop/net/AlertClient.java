package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Alert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlertClient {

    private final ApiClient api;

    public AlertClient(ApiClient api) {
        this.api = api;
    }

    public List<Alert> list(String type, String status) throws ApiException {
        Map<String, String> query = new LinkedHashMap<>();
        if (type != null && !type.isBlank()) {
            query.put("type", type);
        }
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        return JsonLists.mapArray(api.get("/api/alerts", query), Alert::fromJson);
    }

    public Alert getById(long id) throws ApiException {
        return Alert.fromJson(api.get("/api/alerts/" + id));
    }

    public Alert acknowledge(long id) throws ApiException {
        return Alert.fromJson(api.post("/api/alerts/" + id + "/acknowledge", Map.of()));
    }

    public Alert resolve(long id) throws ApiException {
        return Alert.fromJson(api.post("/api/alerts/" + id + "/resolve", Map.of()));
    }

    public Alert ignore(long id) throws ApiException {
        return Alert.fromJson(api.post("/api/alerts/" + id + "/ignore", Map.of()));
    }
}
