package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsTimelinePoint {

    private String label;
    private BigDecimal revenue;
    private long saleCount;
}
