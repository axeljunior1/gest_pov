package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record SaleRefundSummary(
        Long id,
        String refundNumber,
        Long saleId,
        String saleNumber,
        String customerName,
        String status,
        BigDecimal totalAmount,
        String reason,
        String createdBy,
        String createdAt,
        String validatedAt,
        int lineCount
) {
    public static SaleRefundSummary fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new SaleRefundSummary(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "refundNumber"),
                Product.longOrNull(node, "saleId"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "customerName"),
                Product.textOrNull(node, "status"),
                Product.decimalOrNull(node, "totalAmount"),
                Product.textOrNull(node, "reason"),
                Product.textOrNull(node, "createdBy"),
                Product.textOrNull(node, "createdAt"),
                Product.textOrNull(node, "validatedAt"),
                node.path("lineCount").asInt(0)
        );
    }
}
