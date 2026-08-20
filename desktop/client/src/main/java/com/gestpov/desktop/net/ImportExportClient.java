package com.gestpov.desktop.net;

import com.gestpov.desktop.model.ImportJob;
import com.gestpov.desktop.model.ImportPreview;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportExportClient {

    private final ApiClient api;

    public ImportExportClient(ApiClient api) {
        this.api = api;
    }

    public byte[] productTemplate(String format) throws ApiException {
        return api.getBytes("/api/import/templates/products", Map.of("format", format == null ? "CSV" : format));
    }

    public ImportPreview previewProducts(String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("duplicateMode", duplicateMode == null ? "REJECT" : duplicateMode);
        return ImportPreview.fromJson(api.postMultipart("/api/import/products/preview", "file", fileName, bytes, fields));
    }

    public ImportPreview validateProducts(String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("duplicateMode", duplicateMode == null ? "REJECT" : duplicateMode);
        // validate returns ImportValidateResponse — reuse preview fields for MVP summary
        var node = api.postMultipart("/api/import/products/validate", "file", fileName, bytes, fields);
        if (node.has("totalRows")) {
            return ImportPreview.fromJson(node);
        }
        return new ImportPreview(
                node.path("totalRows").asInt(0),
                node.path("successRows").asInt(node.path("validRows").asInt(0)),
                node.path("errorRows").asInt(0)
        );
    }

    public List<ImportJob> history() throws ApiException {
        return JsonLists.mapArray(api.get("/api/import/history"), ImportJob::fromJson);
    }

    public byte[] exportProducts(String format) throws ApiException {
        return api.getBytes("/api/export/products", Map.of("format", format == null ? "CSV" : format));
    }

    public byte[] exportStock(String format) throws ApiException {
        return api.getBytes("/api/export/stock", Map.of("format", format == null ? "CSV" : format));
    }

    public byte[] exportAlerts(String format) throws ApiException {
        return api.getBytes("/api/export/alerts", Map.of("format", format == null ? "CSV" : format));
    }
}
