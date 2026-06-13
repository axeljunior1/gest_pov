package com.erp.products.dto;

import com.erp.products.domain.enums.SaleRefundStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SaleRefundResponse {

    private Long id;
    private String refundNumber;
    private Long saleId;
    private String saleNumber;
    private SaleRefundStatus status;
    private BigDecimal totalAmount;
    private String reason;
    private Boolean returnToStock;
    private String createdBy;
    private Instant createdAt;
    private Instant completedAt;
    private List<SaleRefundLineResponse> lignes;
}
