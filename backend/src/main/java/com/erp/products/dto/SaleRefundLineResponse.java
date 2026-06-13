package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SaleRefundLineResponse {

    private Long id;
    private Long saleLineId;
    private BigDecimal quantity;
    private BigDecimal refundAmount;
}
