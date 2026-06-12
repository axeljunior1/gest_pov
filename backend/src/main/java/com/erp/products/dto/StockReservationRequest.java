package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockReservationRequest {
    @NotNull
    private Long productId;
    private Long variantId;
    @NotNull
    private Long warehouseId;
    @NotNull
    private Long locationId;
    private Long lotId;
    @NotNull
    @Positive
    private BigDecimal quantity;
    private String reference;
    private String utilisateur = "system";
}
