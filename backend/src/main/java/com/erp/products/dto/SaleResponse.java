package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.PaymentStatus;
import com.erp.products.domain.enums.SaleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SaleResponse {

    private Long id;
    private String saleNumber;
    private Long posSessionId;
    private Long paymentSessionId;
    private Long sellerId;
    private String sellerName;
    private Long cashierId;
    private String cashierName;
    private Long warehouseId;
    private SaleStatus status;
    private Long customerId;
    private String customerNumber;
    private String customerName;
    private String customerPhone;
    private Integer customerLoyaltyPoints;
    private String customerLoyaltyTier;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal loyaltyDiscountAmount;
    private Integer loyaltyPointsRedeemed;
    private Integer loyaltyPointsEarned;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String holdLabel;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant sentToPaymentAt;
    private Instant validatedAt;
    private Instant paidAt;
    private Instant cancelledAt;
    private List<SaleLineResponse> lignes;
    private List<PaymentResponse> payments;
    /** Au moins une ligne dépasse le stock disponible (mode stock positif). */
    private Boolean hasStockIssues;
}
