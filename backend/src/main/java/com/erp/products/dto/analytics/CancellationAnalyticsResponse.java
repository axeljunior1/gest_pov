package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CancellationAnalyticsResponse {

    private long cancelledCount;
    private BigDecimal cancelledAmountTotal;
    private AnalyticsComparisonMetric cancelledCountPeriod;
    private AnalyticsComparisonMetric cancelledAmountPeriod;
    private List<CancellationReasonStat> topReasons;
    private List<CancellationActorStat> topSellers;
    private List<CancellationActorStat> topCashiers;
}
