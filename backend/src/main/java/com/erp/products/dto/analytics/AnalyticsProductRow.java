package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsProductRow {

    private Long productId;
    private String productName;
    private String sku;
    private String categoryName;
    private BigDecimal quantitySold;
    private BigDecimal revenue;
    private BigDecimal estimatedMargin;
    private BigDecimal discountAmount;
    private BigDecimal stockRemaining;
    private BigDecimal rotationRate;
}
