package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LotRequest {
    @NotNull
    private Long productId;
    private Long variantId;
    @NotNull
    private String numeroLot;
    private LocalDate datePeremption;
    private LocalDate dateFabrication;
}
