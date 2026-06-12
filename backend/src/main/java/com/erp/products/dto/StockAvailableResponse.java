package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockAvailableResponse {
    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private String unitSymbole;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
}
