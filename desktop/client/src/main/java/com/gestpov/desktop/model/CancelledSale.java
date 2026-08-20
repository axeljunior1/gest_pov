package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record CancelledSale(
        Long id,
        String saleNumber,
        String createdAt,
        String cancelledAt,
        String sellerName,
        String cashierName,
        String customerName,
        BigDecimal total,
        String cancellationReason,
        String cancellationReasonLabel,
        String status
) {
    public static CancelledSale fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new CancelledSale(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "createdAt"),
                Product.textOrNull(node, "cancelledAt"),
                Product.textOrNull(node, "sellerName"),
                Product.textOrNull(node, "cashierName"),
                Product.textOrNull(node, "customerName"),
                Product.decimalOrNull(node, "total"),
                Product.textOrNull(node, "cancellationReason"),
                Product.textOrNull(node, "cancellationReasonLabel"),
                Product.textOrNull(node, "status")
        );
    }
}
