package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsPaymentsResponse {

    private List<AnalyticsPaymentMethodRow> methods;
    private java.math.BigDecimal refundsTotal;
    private java.math.BigDecimal expectedCash;
    private java.math.BigDecimal declaredCash;
    private java.math.BigDecimal cashDifference;
}
