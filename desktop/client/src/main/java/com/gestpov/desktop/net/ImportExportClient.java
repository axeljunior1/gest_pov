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
        return template("products", format);
    }

    public byte[] template(String type, String format) throws ApiException {
        return api.getBytes("/api/import/templates/" + type, Map.of("format", format == null ? "CSV" : format));
    }

    public ImportPreview previewProducts(String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        return preview("products", fileName, bytes, duplicateMode);
    }

    public ImportPreview validateProducts(String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        return validate("products", fileName, bytes, duplicateMode);
    }

    public ImportPreview preview(String type, String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        Map<String, String> fields = new LinkedHashMap<>();
        if (duplicateMode != null) {
            fields.put("duplicateMode", duplicateMode);
        }
        return ImportPreview.fromJson(api.postMultipart("/api/import/" + type + "/preview", "file", fileName, bytes, fields));
    }

    public ImportPreview validate(String type, String fileName, byte[] bytes, String duplicateMode) throws ApiException {
        Map<String, String> fields = new LinkedHashMap<>();
        if (duplicateMode != null) {
            fields.put("duplicateMode", duplicateMode);
        }
        var node = api.postMultipart("/api/import/" + type + "/validate", "file", fileName, bytes, fields);
        if (node.has("totalRows")) {
            return ImportPreview.fromJson(node);
        }
        var job = node.path("job");
        return new ImportPreview(
                job.path("totalRows").asInt(node.path("totalRows").asInt(0)),
                job.path("successRows").asInt(node.path("successRows").asInt(node.path("validRows").asInt(0))),
                job.path("errorRows").asInt(node.path("errorRows").asInt(0))
        );
    }

    public List<ImportJob> history() throws ApiException {
        return JsonLists.mapArray(api.get("/api/import/history"), ImportJob::fromJson);
    }

    public byte[] exportProducts(String format) throws ApiException {
        return export("products", format);
    }

    public byte[] exportStock(String format) throws ApiException {
        return export("stock", format);
    }

    public byte[] exportAlerts(String format) throws ApiException {
        return export("alerts", format);
    }

    public byte[] export(String type, String format) throws ApiException {
        return api.getBytes("/api/export/" + type, Map.of("format", format == null ? "CSV" : format));
    }
}
