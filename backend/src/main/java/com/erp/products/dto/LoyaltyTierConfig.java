package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoyaltyTierConfig {
    private String name;
    private Integer minPoints;
    private BigDecimal discountPercent;
}
