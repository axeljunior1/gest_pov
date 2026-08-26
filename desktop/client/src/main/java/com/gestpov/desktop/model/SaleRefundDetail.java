package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestpov.desktop.net.JsonLists;

import java.math.BigDecimal;
import java.util.List;

public record SaleRefundDetail(
        Long id,
        String refundNumber,
        String saleNumber,
        String customerName,
        String status,
        BigDecimal totalAmount,
        String reason,
        String notes,
        String createdBy,
        String createdAt,
        String validatedAt,
        List<String> lines,
        List<String> payments
) {
    public static SaleRefundDetail fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> lines = JsonLists.mapArray(node.get("lignes"), n -> {
            String nom = Product.textOrNull(n, "productNom");
            BigDecimal qty = Product.decimalOrNull(n, "quantity");
            BigDecimal amount = Product.decimalOrNull(n, "refundAmount");
            return (nom == null ? "?" : nom)
                    + (qty == null ? "" : " × " + qty.stripTrailingZeros().toPlainString())
                    + (amount == null ? "" : " = " + amount);
        });
        List<String> payments = JsonLists.mapArray(node.get("payments"), n -> {
            String method = Product.textOrNull(n, "method");
            BigDecimal amount = Product.decimalOrNull(n, "amount");
            String status = Product.textOrNull(n, "status");
            return (method == null ? "?" : method)
                    + (amount == null ? "" : " — " + amount)
                    + (status == null ? "" : " (" + status + ")");
        });
        return new SaleRefundDetail(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "refundNumber"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "customerName"),
                Product.textOrNull(node, "status"),
                Product.decimalOrNull(node, "totalAmount"),
                Product.textOrNull(node, "reason"),
                Product.textOrNull(node, "notes"),
                Product.textOrNull(node, "createdBy"),
                Product.textOrNull(node, "createdAt"),
                Product.textOrNull(node, "validatedAt"),
                lines,
                payments
        );
    }
}
