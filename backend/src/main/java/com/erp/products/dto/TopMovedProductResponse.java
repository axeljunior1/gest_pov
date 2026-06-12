package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopMovedProductResponse {

    private Long productId;
    private String productNom;
    private long movementCount;
    private BigDecimal totalQuantityMoved;
}
