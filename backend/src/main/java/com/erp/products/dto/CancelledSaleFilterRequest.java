package com.erp.products.dto;

import com.erp.products.domain.enums.SaleCancellationReason;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CancelledSaleFilterRequest {

    private String period;
    private Long sellerId;
    private Long cashierId;
    private Long customerId;
    private SaleCancellationReason reason;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private Integer page;
    private Integer size;
}
