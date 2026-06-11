package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitConversionRequest {

    @NotNull
    private Long fromUnitId;

    @NotNull
    private Long toUnitId;

    @NotNull
    @Positive
    private BigDecimal factor;
}
