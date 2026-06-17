package com.erp.products.dto.stockvaluation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockValuationCategoryRow {
    private Long categoryId;
    private String categoryName;
    private BigDecimal stockValue;
}
