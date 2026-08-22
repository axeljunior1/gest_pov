package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.AppSetting;
import com.gestpov.desktop.model.ClientConfiguration;
import com.gestpov.desktop.model.ReferenceValueOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    /** Sans restriction de permission (contrairement a /api/settings) : lisible par tout utilisateur connecte. */
    public String getPublicCurrency() throws ApiException {
        JsonNode node = api.get("/api/settings/public");
        return node != null && node.hasNonNull("currency") ? node.get("currency").asText(null) : null;
    }

    public ClientConfiguration getClientConfig() throws ApiException {
        return ClientConfiguration.fromJson(api.get("/api/settings/client-config"));
    }

    public ClientConfiguration updateClientConfig(Map<String, Object> body) throws ApiException {
        return ClientConfiguration.fromJson(api.put("/api/settings/client-config", body));
    }

    public ClientConfiguration uploadCompanyLogo(String fileName, byte[] bytes) throws ApiException {
        return ClientConfiguration.fromJson(api.postMultipart(
                "/api/settings/company/logo", "file", fileName, bytes, Map.of()));
    }

    /**
     * Listes de valeurs groupées par catégorie (CURRENCY, LANGUAGE, …).
     */
    public Map<String, List<ReferenceValueOption>> getReferenceValues() throws ApiException {
        JsonNode root = api.get("/api/settings/reference-values");
        if (root == null || !root.isObject()) {
            return Map.of();
        }
        Map<String, List<ReferenceValueOption>> out = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> {
            List<ReferenceValueOption> options = new ArrayList<>();
            if (entry.getValue() != null && entry.getValue().isArray()) {
                for (JsonNode n : entry.getValue()) {
                    ReferenceValueOption opt = ReferenceValueOption.fromJson(n);
                    if (opt != null && opt.code() != null && !opt.code().isBlank()) {
                        options.add(opt);
                    }
                }
            }
            out.put(entry.getKey(), Collections.unmodifiableList(options));
        });
        return out;
    }

    public AppSetting update(String key, String value) throws ApiException {
        return AppSetting.fromJson(api.put("/api/settings/" + key, Map.of("value", value == null ? "" : value)));
    }

    public List<AppSetting> updateBulk(Map<String, String> values) throws ApiException {
        return JsonLists.mapArray(
                api.put("/api/settings", Map.of("settings", values == null ? Map.of() : values)),
                AppSetting::fromJson
        );
    }

    public String installationId() throws ApiException {
        JsonNode node = api.get("/api/license/installation-id");
        if (node == null || !node.hasNonNull("installationId")) {
            return "";
        }
        return node.get("installationId").asText("");
    }
}
