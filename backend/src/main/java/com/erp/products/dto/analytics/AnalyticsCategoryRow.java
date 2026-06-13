package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsCategoryRow {

    private Long categoryId;
    private String categoryName;
    private BigDecimal revenue;
    private BigDecimal quantitySold;
    private BigDecimal estimatedMargin;
    private BigDecimal sharePercent;
}
