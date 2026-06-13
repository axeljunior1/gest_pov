package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsTopCustomerRow {

    private Long customerId;
    private String customerName;
    private String customerNumber;
    private BigDecimal revenue;
    private long purchaseCount;
    private BigDecimal averageBasket;
}
