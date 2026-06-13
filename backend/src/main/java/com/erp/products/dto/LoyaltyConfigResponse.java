package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LoyaltyConfigResponse {
    private boolean loyaltyEnabled;
    private BigDecimal pointsPerCurrencyUnit;
    private BigDecimal currencyUnitAmount;
    private BigDecimal pointValue;
    private Integer minimumPointsToRedeem;
    private BigDecimal maximumDiscountPercent;
    private boolean pointsExpirationEnabled;
    private Integer pointsExpirationDays;
    private boolean earnPointsOnDiscountedSales;
    private boolean earnPointsOnTaxIncludedAmount;
    private boolean allowPointsRedemption;
    private List<LoyaltyTierConfig> loyaltyTiersConfig;
}
