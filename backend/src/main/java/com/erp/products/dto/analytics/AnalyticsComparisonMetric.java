package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsComparisonMetric {

    private BigDecimal current;
    private BigDecimal previous;
    private BigDecimal changePercent;
    private String trend;
}
