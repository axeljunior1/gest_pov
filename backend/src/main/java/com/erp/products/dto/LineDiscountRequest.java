package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LineDiscountRequest {

    private BigDecimal discountAmount;

    private String managerEmail;
    private String managerPassword;
    private String managerBadgeCode;
    private String managerPin;
}
