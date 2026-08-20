package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record SaleSummary(
        Long id,
        String saleNumber,
        String status,
        String createdAt,
        String paidAt,
        String customerName,
        String sellerName,
        String cashierName,
        BigDecimal total,
        BigDecimal paidAmount,
        int refundCount,
        BigDecimal totalRefunded
) {
    public static SaleSummary fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new SaleSummary(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "status"),
                Product.textOrNull(node, "createdAt"),
                Product.textOrNull(node, "paidAt"),
                Product.textOrNull(node, "customerName"),
                Product.textOrNull(node, "sellerName"),
                Product.textOrNull(node, "cashierName"),
                Product.decimalOrNull(node, "total"),
                Product.decimalOrNull(node, "paidAmount"),
                node.path("refundCount").asInt(0),
                Product.decimalOrNull(node, "totalRefunded")
        );
    }
}
