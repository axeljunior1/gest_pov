package com.erp.products.dto.analytics;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AnalyticsFilterRequest {

    /** TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM */
    private String period;

    private Instant dateFrom;
    private Instant dateTo;

    private Long warehouseId;
    private Long cashierId;
    private Long categoryId;
    private Long productId;
    private Long customerId;
    private PaymentMethod paymentMethod;

    /** HOUR, DAY, MONTH — pour la timeline */
    private String granularity;

    private Integer page;
    private Integer size;
    private String sort;
}
