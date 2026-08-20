package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record DashboardSummary(
        long totalProducts,
        BigDecimal totalStockQuantity,
        BigDecimal stockValue,
        String stockValuationMethod,
        long outOfStockProducts,
        long lowStockProducts
) {
    public static DashboardSummary fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new DashboardSummary(
                node.path("totalProducts").asLong(0),
                Product.decimalOrNull(node, "totalStockQuantity"),
                Product.decimalOrNull(node, "stockValue"),
                Product.textOrNull(node, "stockValuationMethod"),
                node.path("outOfStockProducts").asLong(0),
                node.path("lowStockProducts").asLong(0)
        );
    }
}
