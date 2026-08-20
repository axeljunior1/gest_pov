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
        Long customerId,
        String customerName,
        String customerPhone,
        Integer customerLoyaltyPoints,
        String sellerName,
        List<SaleLine> lignes
) {
    public static Sale fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String seller = Product.textOrNull(node, "sellerName");
        if (seller == null) {
            seller = Product.textOrNull(node, "cashierName");
        }
        return new Sale(
                Product.longOrNull(node, "id"),
                Product.textOrNull(node, "saleNumber"),
                Product.textOrNull(node, "status"),
                Product.decimalOrNull(node, "total"),
                Product.decimalOrNull(node, "discountTotal"),
                node.hasNonNull("hasStockIssues") && node.get("hasStockIssues").asBoolean(),
                Product.longOrNull(node, "customerId"),
                Product.textOrNull(node, "customerName"),
                Product.textOrNull(node, "customerPhone"),
                node.hasNonNull("customerLoyaltyPoints") ? node.get("customerLoyaltyPoints").asInt() : null,
                seller,
                com.gestpov.desktop.net.JsonLists.mapArray(node.get("lignes"), SaleLine::fromJson)
        );
    }

    public boolean hasCustomer() {
        return customerId != null;
    }
}
