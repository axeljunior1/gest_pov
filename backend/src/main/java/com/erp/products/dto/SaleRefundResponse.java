package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.RefundFulfillmentStatus;
import com.erp.products.domain.enums.RefundPaymentStatus;
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
    private String customerName;
    private SaleRefundStatus status;
    private RefundFulfillmentStatus refundStatus;
    private BigDecimal totalAmount;
    private String reason;
    private String notes;
    private Boolean returnToStock;
    private String createdBy;
    private Instant createdAt;
    private Instant validatedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private List<SaleRefundLineResponse> lignes;
    private List<RefundPaymentResponse> payments;
}
