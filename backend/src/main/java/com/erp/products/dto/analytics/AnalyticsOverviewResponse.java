package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsOverviewResponse {

    private AnalyticsComparisonMetric revenueToday;
    private AnalyticsComparisonMetric revenueWeek;
    private AnalyticsComparisonMetric revenueMonth;

    private AnalyticsComparisonMetric salesCountToday;
    private AnalyticsComparisonMetric averageBasketToday;
    private AnalyticsComparisonMetric itemsSoldToday;

    private BigDecimal refundsTotal;
    private BigDecimal discountsTotal;
    private BigDecimal grossProfitEstimate;

    private AnalyticsComparisonMetric refundsPeriod;
    private AnalyticsComparisonMetric discountsPeriod;

    private AnalyticsComparisonMetric cancelledCountPeriod;
    private AnalyticsComparisonMetric cancelledAmountPeriod;
    private BigDecimal cancelledAmountTotal;

    private String periodLabel;
    private String currency;
}
