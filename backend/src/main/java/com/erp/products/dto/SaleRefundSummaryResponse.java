package com.erp.products.dto;

import com.erp.products.domain.enums.SaleRefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SaleRefundSummaryResponse {

    private Long id;
    private String refundNumber;
    private Long saleId;
    private String saleNumber;
    private String customerName;
    private SaleRefundStatus status;
    private BigDecimal totalAmount;
    private String reason;
    private String createdBy;
    private Instant createdAt;
    private Instant validatedAt;
    private int lineCount;
}
