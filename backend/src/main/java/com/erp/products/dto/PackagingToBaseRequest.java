package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackagingToBaseRequest {

    @NotNull
    private Long packagingId;

    @NotNull
    @Positive
    private BigDecimal quantity;
}
