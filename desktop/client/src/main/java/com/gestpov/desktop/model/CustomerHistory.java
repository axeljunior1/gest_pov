package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record CustomerHistory(
        Long customerId,
        String customerNumber,
        String fullName,
        Integer loyaltyPoints,
        String loyaltyTier,
        long purchaseCount,
        BigDecimal totalSpent,
        BigDecimal averageBasket,
        String lastPurchaseAt,
        Integer totalPointsEarned,
        Integer totalPointsRedeemed,
        List<LoyaltyTxn> recentTransactions
) {

    public static CustomerHistory fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<LoyaltyTxn> txns = new ArrayList<>();
        JsonNode recent = node.get("recentTransactions");
        if (recent != null && recent.isArray()) {
            recent.forEach(item -> {
                LoyaltyTxn txn = LoyaltyTxn.fromJson(item);
                if (txn != null) {
                    txns.add(txn);
                }
            });
        }
        return new CustomerHistory(
                node.hasNonNull("customerId") ? node.get("customerId").asLong() : null,
                textOrNull(node, "customerNumber"),
                textOrNull(node, "fullName"),
                node.hasNonNull("loyaltyPoints") ? node.get("loyaltyPoints").asInt() : 0,
                textOrNull(node, "loyaltyTier"),
                node.path("purchaseCount").asLong(0),
                decimalOrNull(node, "totalSpent"),
                decimalOrNull(node, "averageBasket"),
                textOrNull(node, "lastPurchaseAt"),
                node.hasNonNull("totalPointsEarned") ? node.get("totalPointsEarned").asInt() : 0,
                node.hasNonNull("totalPointsRedeemed") ? node.get("totalPointsRedeemed").asInt() : 0,
                List.copyOf(txns)
        );
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static BigDecimal decimalOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        try {
            return new BigDecimal(node.get(field).asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record LoyaltyTxn(Long id, String type, Integer points, String createdAt, String saleNumber) {
        public static LoyaltyTxn fromJson(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            return new LoyaltyTxn(
                    node.hasNonNull("id") ? node.get("id").asLong() : null,
                    node.hasNonNull("type") ? node.get("type").asText() : "",
                    node.hasNonNull("points") ? node.get("points").asInt() : 0,
                    node.hasNonNull("createdAt") ? node.get("createdAt").asText() : "",
                    node.hasNonNull("saleNumber") ? node.get("saleNumber").asText() : ""
            );
        }
    }
}
