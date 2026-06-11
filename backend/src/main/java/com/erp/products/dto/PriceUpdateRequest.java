package com.erp.products.dto;

import com.erp.products.domain.enums.PriceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceUpdateRequest {

    @NotNull
    private PriceType type;

    @NotNull
    private BigDecimal nouveauPrix;

    private String utilisateur;
}
