package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.RefundPaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class RefundPaymentResponse {

    private Long id;
    private PaymentMethod method;
    private BigDecimal amount;
    private RefundPaymentStatus status;
    private Instant refundedAt;
    private Long originalPaymentId;
}
