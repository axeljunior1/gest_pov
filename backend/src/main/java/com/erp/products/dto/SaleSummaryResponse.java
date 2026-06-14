package com.erp.products.dto;

import com.erp.products.domain.enums.SaleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SaleSummaryResponse {

    private Long id;
    private String saleNumber;
    private SaleStatus status;
    private Instant createdAt;
    private Instant paidAt;
    private Instant validatedAt;
    private String customerName;
    private String sellerName;
    private String cashierName;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private int refundCount;
    private BigDecimal totalRefunded;
}
