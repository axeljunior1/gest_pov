package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.model.DashboardAlertSummary;
import com.gestpov.desktop.model.DashboardSummary;
import com.gestpov.desktop.model.StockMovement;

import java.util.List;
import java.util.Map;

public class DashboardClient {

    private final ApiClient api;

    public DashboardClient(ApiClient api) {
        this.api = api;
    }

    public DashboardSummary summary() throws ApiException {
        return DashboardSummary.fromJson(api.get("/api/dashboard/summary"));
    }

    public DashboardAlertSummary alerts() throws ApiException {
        return DashboardAlertSummary.fromJson(api.get("/api/dashboard/alerts"));
    }

    public List<StockMovement> recentMovements(int limit) throws ApiException {
        return JsonLists.mapArray(
                api.get("/api/dashboard/movements/recent", Map.of("limit", String.valueOf(limit))),
                StockMovement::fromJson);
    }

    public List<String> recentEntries(int limit) throws ApiException {
        return JsonLists.mapArray(
                api.get("/api/dashboard/entries/recent", Map.of("limit", String.valueOf(limit))),
                DashboardClient::entryLabel);
    }

    public List<String> recentExits(int limit) throws ApiException {
        return JsonLists.mapArray(
                api.get("/api/dashboard/exits/recent", Map.of("limit", String.valueOf(limit))),
                DashboardClient::exitLabel);
    }

    public List<String> topMoved(int limit) throws ApiException {
        return JsonLists.mapArray(
                api.get("/api/dashboard/products/top-moved", Map.of("limit", String.valueOf(limit))),
                n -> {
                    String name = n.path("productNom").asText(n.path("productName").asText("?"));
                    return name + " (" + n.path("movementCount").asText("0") + ")";
                });
    }

    public List<String> warehouses() throws ApiException {
        return JsonLists.mapArray(api.get("/api/dashboard/warehouses"), n ->
                n.path("warehouseCode").asText("?") + " — qty "
                        + n.path("totalQuantity").asText("0"));
    }

    public JsonNode health() throws ApiException {
        return api.get("/api/health");
    }

    public JsonNode discovery() throws ApiException {
        return api.get("/api/discovery");
    }

    private static String entryLabel(JsonNode n) {
        return n.path("documentNumber").asText("?") + " · "
                + n.path("productNom").asText("") + " · "
                + n.path("quantity").asText("");
    }

    private static String exitLabel(JsonNode n) {
        return n.path("documentNumber").asText("?") + " · "
                + n.path("productNom").asText("") + " · "
                + n.path("quantity").asText("");
    }
}
