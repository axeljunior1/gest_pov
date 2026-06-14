package com.erp.products.dto;

import com.erp.products.domain.enums.StockValuationMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStockSummaryResponse {

    private long totalProducts;
    private BigDecimal totalStockQuantity;
    private BigDecimal stockValue;
    private StockValuationMethod stockValuationMethod;
    private long outOfStockProducts;
    private long lowStockProducts;
}
