package com.gestpov.desktop.net;

import com.gestpov.desktop.model.SaleBrowsePage;
import com.gestpov.desktop.model.SaleDetail;

import java.util.LinkedHashMap;
import java.util.Map;

public class SalesBrowseClient {

    private final ApiClient api;

    public SalesBrowseClient(ApiClient api) {
        this.api = api;
    }

    public SaleBrowsePage browse(String q, String status, int page, int limit) throws ApiException {
        Map<String, String> query = new LinkedHashMap<>();
        if (q != null && !q.isBlank()) {
            query.put("q", q);
        }
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        query.put("page", String.valueOf(Math.max(page, 0)));
        query.put("limit", String.valueOf(limit <= 0 ? 50 : limit));
        return SaleBrowsePage.fromJson(api.get("/api/sales/browse", query));
    }

    public SaleDetail detail(long id) throws ApiException {
        return SaleDetail.fromJson(api.get("/api/sales/" + id));
    }

    public byte[] exportCsv(String q, String status) throws ApiException {
        Map<String, String> query = new LinkedHashMap<>();
        if (q != null && !q.isBlank()) {
            query.put("q", q);
        }
        if (status != null && !status.isBlank()) {
            query.put("status", status);
        }
        return api.getBytes("/api/sales/browse/export", query);
    }
}
