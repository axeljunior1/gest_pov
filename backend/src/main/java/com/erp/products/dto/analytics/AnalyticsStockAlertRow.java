package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsStockAlertRow {

    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal stockRemaining;
    private BigDecimal quantitySoldPeriod;
    private String alertType;
    private String severity;
}
