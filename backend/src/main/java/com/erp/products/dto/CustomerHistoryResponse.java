package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CustomerHistoryResponse {
    private Long customerId;
    private String customerNumber;
    private String fullName;
    private Integer loyaltyPoints;
    private String loyaltyTier;
    private long purchaseCount;
    private BigDecimal totalSpent;
    private BigDecimal averageBasket;
    private Instant lastPurchaseAt;
    private Integer totalPointsEarned;
    private Integer totalPointsRedeemed;
    private List<TopProduct> topProducts;
    private List<LoyaltyTransactionResponse> recentTransactions;

    @Data
    @Builder
    public static class TopProduct {
        private Long productId;
        private String productNom;
        private BigDecimal totalQuantity;
        private BigDecimal totalAmount;
    }
}
