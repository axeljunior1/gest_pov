package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStockSummaryResponse {

    private long totalProducts;
    private BigDecimal totalStockQuantity;
    private BigDecimal stockValue;
    private long outOfStockProducts;
    private long lowStockProducts;
}
