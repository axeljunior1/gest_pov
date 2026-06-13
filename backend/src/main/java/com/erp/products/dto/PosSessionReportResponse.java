package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PosSessionReportResponse {

    private Long sessionId;
    private String sessionNumber;
    private int saleCount;
    private BigDecimal totalRevenue;
    private BigDecimal cashRevenue;
    private BigDecimal cardRevenue;
    private BigDecimal mobileMoneyRevenue;
    private BigDecimal refundsTotal;
    private BigDecimal expectedCashAmount;
    private BigDecimal declaredCashAmount;
    private BigDecimal cashDifference;
}
