package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AnalyticsCustomerSummaryResponse {

    private long newCustomers;
    private long returningCustomers;
    private List<AnalyticsTopCustomerRow> topCustomers;
    private BigDecimal averageBasketPerCustomer;
    private long loyaltyPointsEarned;
    private long loyaltyPointsRedeemed;
}
