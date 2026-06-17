package com.erp.products.dto.stockvaluation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockValuationStaleProductRow {
    private Long productId;
    private Long variantId;
    private String productName;
    private BigDecimal quantityOnHand;
    private BigDecimal stockValue;
    private Instant lastMovementAt;
}
