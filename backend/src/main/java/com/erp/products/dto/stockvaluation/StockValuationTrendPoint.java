package com.erp.products.dto.stockvaluation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockValuationTrendPoint {
    private String period;
    private BigDecimal value;
}
