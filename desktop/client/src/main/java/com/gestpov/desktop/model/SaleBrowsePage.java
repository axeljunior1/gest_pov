package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.util.List;

public record SaleBrowsePage(
        List<SaleSummary> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public static SaleBrowsePage fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return new SaleBrowsePage(List.of(), 0, 0, 0, 0);
        }
        return new SaleBrowsePage(
                JsonLists.mapArray(node.get("items"), SaleSummary::fromJson),
                node.path("totalElements").asLong(0),
                node.path("page").asInt(0),
                node.path("size").asInt(0),
                node.path("totalPages").asInt(0)
        );
    }
}
