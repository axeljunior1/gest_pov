package com.erp.products.dto;

import com.erp.products.domain.enums.LoyaltyTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class LoyaltyTransactionResponse {
    private Long id;
    private Long customerId;
    private Long saleId;
    private String saleNumber;
    private LoyaltyTransactionType type;
    private Integer points;
    private BigDecimal amountValue;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String createdBy;
    private Instant createdAt;
}
