package com.erp.products.dto.stockvaluation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class StockValuationOverviewResponse {
    private BigDecimal totalStockValue;
    private List<StockValuationCategoryRow> byCategory;
}
