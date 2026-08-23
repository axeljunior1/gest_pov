package com.gestpov.desktop.net;

import java.util.LinkedHashMap;
import java.util.Map;

public class BarcodeClient {

    private final ApiClient api;

    public BarcodeClient(ApiClient api) {
        this.api = api;
    }

    /** Retourne le PNG encodé en base64 pour le contenu et le type donnés (EAN13, UPC, CODE128, QR_CODE). */
    public String generateBase64(String content, String type) throws ApiException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", content);
        body.put("type", type);
        return api.post("/api/barcodes/generate", body).path("imageBase64").asText(null);
    }
}
