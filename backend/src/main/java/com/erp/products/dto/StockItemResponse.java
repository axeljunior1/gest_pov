package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockItemResponse {
    private Long id;
    private Long productId;
    private String productNom;
    private Long variantId;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private Long lotId;
    private String lotNumero;
    private String unitSymbole;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
}
