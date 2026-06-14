package com.erp.products.dto;

import com.erp.products.domain.enums.SaleCancellationReason;
import com.erp.products.domain.enums.SaleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class CancelledSaleSummaryResponse {

    private Long id;
    private String saleNumber;
    private Instant createdAt;
    private Instant cancelledAt;
    private String sellerName;
    private String cashierName;
    private String customerName;
    private BigDecimal total;
    private SaleCancellationReason cancellationReason;
    private String cancellationReasonLabel;
    private SaleStatus status;
}
