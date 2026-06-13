package com.erp.products.dto;

import lombok.Data;

@Data
public class LoyaltyAdjustRequest {
    private Integer points;
    private String reason;
}
