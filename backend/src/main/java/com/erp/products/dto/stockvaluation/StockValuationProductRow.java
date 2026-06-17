package com.erp.products.dto.stockvaluation;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockValuationProductRow {
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantLabel;
    private BigDecimal quantityOnHand;
    private BigDecimal averageUnitCost;
    private BigDecimal stockValue;
    /** Marge unitaire si prix de vente connu (optionnel). */
    private BigDecimal unitMargin;
}
