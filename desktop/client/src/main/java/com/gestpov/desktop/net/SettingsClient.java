package com.gestpov.desktop.net;

import com.gestpov.desktop.model.AppSetting;

import java.util.List;
import java.util.Map;

public class SettingsClient {

    private final ApiClient api;

    public SettingsClient(ApiClient api) {
        this.api = api;
    }

    public List<AppSetting> getAll() throws ApiException {
        return JsonLists.mapArray(api.get("/api/settings"), AppSetting::fromJson);
    }

    public AppSetting update(String key, String value) throws ApiException {
        return AppSetting.fromJson(api.put("/api/settings/" + key, Map.of("value", value == null ? "" : value)));
    }
}
