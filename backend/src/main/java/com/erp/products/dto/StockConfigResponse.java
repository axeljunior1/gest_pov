package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockConfigResponse {

    private boolean allowNegativeStock;
    private BigDecimal lowStockThresholdDefault;
}
