package com.gestpov.desktop.net;

import com.gestpov.desktop.model.LicenseStatus;

public class LicenseClient {

    private final ApiClient api;

    public LicenseClient(ApiClient api) {
        this.api = api;
    }

    public LicenseStatus status() throws ApiException {
        return LicenseStatus.fromJson(api.get("/api/license/status"));
    }

    public String installationId() throws ApiException {
        var node = api.get("/api/license/installation-id");
        return node == null || !node.hasNonNull("installationId") ? "" : node.get("installationId").asText("");
    }

    public LicenseStatus importLicense(String fileName, byte[] bytes) throws ApiException {
        return LicenseStatus.fromJson(api.postMultipart("/api/license/import", "file", fileName, bytes, null));
    }
}
