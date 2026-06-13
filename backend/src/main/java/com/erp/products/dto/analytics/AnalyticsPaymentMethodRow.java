package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsPaymentMethodRow {

    private String method;
    private String methodLabel;
    private BigDecimal total;
    private long transactionCount;
    private BigDecimal sharePercent;
}
