package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSessionCloseRequest {

    private BigDecimal closingCashAmount;
}
