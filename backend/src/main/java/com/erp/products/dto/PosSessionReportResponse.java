package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PosSessionReportResponse {

    private Long sessionId;
    private String sessionNumber;
    private String cashierName;
    private Instant openedAt;
    private Instant closedAt;
    private String closedBy;

    private int saleCount;
    private BigDecimal totalRevenue;
    private BigDecimal cashRevenue;
    private BigDecimal cardRevenue;
    private BigDecimal mobileMoneyRevenue;
    private BigDecimal bankTransferRevenue;
    private BigDecimal refundsTotal;
    private BigDecimal cashRefundTotal;

    private BigDecimal openingCashAmount;
    private BigDecimal expectedCashAmount;
    private BigDecimal declaredCashAmount;
    private BigDecimal cashDifference;

    private String differenceReason;
    private String differenceReasonLabel;
    private String differenceComment;
    private String managerValidatedBy;
    private Instant managerValidatedAt;

    /** BALANCED, MINOR, MAJOR — pour code couleur UI. */
    private String differenceSeverity;
    private boolean balanced;

    private boolean requireManagerValidationForDifference;
    private List<CashDifferenceReasonOption> differenceReasonOptions;
    private Integer alertCashDifferenceThreshold;
}
