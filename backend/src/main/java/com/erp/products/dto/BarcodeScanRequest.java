package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BarcodeScanRequest {

    private String code;
    private BigDecimal quantityInput;
}
