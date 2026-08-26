package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.util.List;

public record SaleRefundBrowsePage(
        List<SaleRefundSummary> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public static SaleRefundBrowsePage fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return new SaleRefundBrowsePage(List.of(), 0, 0, 0, 0);
        }
        return new SaleRefundBrowsePage(
                JsonLists.mapArray(node.get("items"), SaleRefundSummary::fromJson),
                node.path("totalElements").asLong(0),
                node.path("page").asInt(0),
                node.path("size").asInt(0),
                node.path("totalPages").asInt(0)
        );
    }
}
