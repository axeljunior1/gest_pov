package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsCashierRow {

    private Long cashierId;
    private String cashierName;
    private long saleCount;
    private BigDecimal revenue;
    private BigDecimal averageBasket;
    private BigDecimal discountsGranted;
    private BigDecimal refundsTotal;
    private long cancellations;
    private BigDecimal cashDifference;
}
