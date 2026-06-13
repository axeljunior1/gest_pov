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
    private Long cashierId;
    private String cashierName;
    private Long warehouseId;
    private SaleStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;
    private String holdLabel;
    private Instant createdAt;
    private Instant validatedAt;
    private Instant cancelledAt;
    private List<SaleLineResponse> lignes;
    private List<PaymentResponse> payments;
}
