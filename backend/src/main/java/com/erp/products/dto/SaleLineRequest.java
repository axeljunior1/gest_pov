package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleLineRequest {

    private Long productId;
    private Long variantId;
    private Long packagingId;
    private BigDecimal quantityInput;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
}
