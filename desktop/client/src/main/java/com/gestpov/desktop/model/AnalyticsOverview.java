package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record AnalyticsOverview(
        BigDecimal revenueToday,
        BigDecimal revenueWeek,
        BigDecimal revenueMonth,
        BigDecimal salesCountToday,
        BigDecimal averageBasketToday,
        BigDecimal refundsTotal,
        BigDecimal discountsTotal,
        BigDecimal cancelledAmountTotal,
        String periodLabel,
        String currency
) {
    public static AnalyticsOverview fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new AnalyticsOverview(
                metricCurrent(node.get("revenueToday")),
                metricCurrent(node.get("revenueWeek")),
                metricCurrent(node.get("revenueMonth")),
                metricCurrent(node.get("salesCountToday")),
                metricCurrent(node.get("averageBasketToday")),
                Product.decimalOrNull(node, "refundsTotal"),
                Product.decimalOrNull(node, "discountsTotal"),
                Product.decimalOrNull(node, "cancelledAmountTotal"),
                Product.textOrNull(node, "periodLabel"),
                Product.textOrNull(node, "currency")
        );
    }

    private static BigDecimal metricCurrent(JsonNode metric) {
        if (metric == null || metric.isNull()) {
            return null;
        }
        return Product.decimalOrNull(metric, "current");
    }
}
