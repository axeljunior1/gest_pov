package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record StockValuationOverview(
        BigDecimal totalStockValue,
        List<CategoryRow> byCategory
) {

    public record CategoryRow(Long categoryId, String categoryName, BigDecimal stockValue) {
        public static CategoryRow fromJson(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            return new CategoryRow(
                    Product.longOrNull(node, "categoryId"),
                    Product.textOrNull(node, "categoryName"),
                    Product.decimalOrNull(node, "stockValue")
            );
        }
    }

    public static StockValuationOverview fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return new StockValuationOverview(BigDecimal.ZERO, List.of());
        }
        List<CategoryRow> rows = new ArrayList<>();
        JsonNode cats = node.get("byCategory");
        if (cats != null && cats.isArray()) {
            cats.forEach(c -> {
                CategoryRow row = CategoryRow.fromJson(c);
                if (row != null) {
                    rows.add(row);
                }
            });
        }
        BigDecimal total = Product.decimalOrNull(node, "totalStockValue");
        return new StockValuationOverview(total == null ? BigDecimal.ZERO : total, List.copyOf(rows));
    }
}
