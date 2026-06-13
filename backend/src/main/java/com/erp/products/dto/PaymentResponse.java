package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private PaymentMethod method;
    private BigDecimal amount;
    private PaymentStatus status;
    private Instant paidAt;
    private Long cashierId;
    private Long posSessionId;
}
