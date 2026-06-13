package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSessionOpenRequest {

    private Long warehouseId;
    private BigDecimal openingCashAmount;
}
