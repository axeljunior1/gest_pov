package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class CancelledSalePaymentInfo {

    private boolean paymentStarted;
    private boolean paymentValidated;
    private String paymentMethod;
    private String paymentMethodLabel;
    private BigDecimal paidAmount;
}
