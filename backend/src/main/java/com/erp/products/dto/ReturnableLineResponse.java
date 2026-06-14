package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ReturnableLineResponse {

    private Long saleLineId;
    private Long productId;
    private Long variantId;
    private String productNom;
    private String variantNameSnapshot;
    private String productSku;
    private Long packagingId;
    private String packagingNameSnapshot;
    private BigDecimal packagingQuantitySnapshot;
    private BigDecimal quantitySold;
    private BigDecimal quantityAlreadyReturned;
    private BigDecimal quantityReturnable;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal lineTotal;
    private BigDecimal maxRefundAmount;
}
