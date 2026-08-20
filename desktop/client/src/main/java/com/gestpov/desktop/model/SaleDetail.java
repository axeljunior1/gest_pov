package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.math.BigDecimal;
import java.util.List;

public record SaleDetail(
        Sale sale,
        BigDecimal totalRefunded,
        List<String> refundSummaries,
        List<String> timeline
) {
    public static SaleDetail fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Sale sale = Sale.fromJson(node.has("sale") ? node.get("sale") : node);
        List<String> refunds = JsonLists.mapArray(node.get("refunds"), n -> {
            String num = Product.textOrNull(n, "refundNumber");
            BigDecimal amt = Product.decimalOrNull(n, "total");
            return (num == null ? "#" + Product.longOrNull(n, "id") : num)
                    + (amt == null ? "" : " — " + amt);
        });
        List<String> timeline = JsonLists.mapArray(node.get("timeline"), n -> {
            String type = Product.textOrNull(n, "eventType");
            String at = Product.textOrNull(n, "createdAt");
            return (type == null ? "?" : type) + (at == null ? "" : " @ " + at);
        });
        return new SaleDetail(
                sale,
                Product.decimalOrNull(node, "totalRefunded"),
                refunds,
                timeline
        );
    }
}
