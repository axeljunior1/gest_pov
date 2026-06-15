package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SaleRefundLineResponse {

    private Long id;
    private Long saleLineId;
    private Long productId;
    private String productNom;
    private String variantNameSnapshot;
    private Long packagingId;
    private String packagingNameSnapshot;
    private BigDecimal quantity;
    private BigDecimal quantityInBaseUnit;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal refundAmount;
    private Boolean restock;
    private String reason;
    private String notes;
}
