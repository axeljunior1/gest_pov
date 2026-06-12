package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WarehouseStockSummaryResponse {

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseNom;
    private BigDecimal totalQuantity;
    private BigDecimal stockValue;
}
