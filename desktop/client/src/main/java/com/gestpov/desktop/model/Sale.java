package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record Sale(
        Long id,
        String saleNumber,
        String status,
        BigDecimal total,
        BigDecimal discountTotal,
        Boolean hasStockIssues,
        List<SaleLine> lignes
) {
    public static Sale fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Sale(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "status"),
                Product.decimalOrNull(node, "total"),
                Product.decimalOrNull(node, "discountTotal"),
                node.hasNonNull("hasStockIssues") && node.get("hasStockIssues").asBoolean(),
                com.gestpov.desktop.net.JsonLists.mapArray(node.get("lignes"), SaleLine::fromJson)
        );
    }
}
