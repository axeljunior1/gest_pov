package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UnitConversionResponse {

    private Long id;
    private Long fromUnitId;
    private String fromUnitSymbole;
    private Long toUnitId;
    private String toUnitSymbole;
    private BigDecimal factor;
}
