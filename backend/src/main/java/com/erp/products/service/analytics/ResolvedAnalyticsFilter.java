package com.erp.products.service.analytics;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ResolvedAnalyticsFilter {

    private Instant from;
    private Instant to;
    private Instant compareFrom;
    private Instant compareTo;

    private Long warehouseId;
    private Long cashierId;
    private Long categoryId;
    private Long productId;
    private Long customerId;
    private PaymentMethod paymentMethod;

    private String granularity;
    private int page;
    private int size;
    private String sort;

    /** Utilisateur limité à ses propres ventes (caissier). */
    private boolean cashierScoped;
    private Long scopedCashierId;
}
