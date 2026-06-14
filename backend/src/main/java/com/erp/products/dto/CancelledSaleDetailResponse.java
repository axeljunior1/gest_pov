package com.erp.products.dto;

import com.erp.products.domain.enums.SaleCancellationReason;
import com.erp.products.domain.enums.SaleEventType;
import com.erp.products.domain.enums.SaleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CancelledSaleDetailResponse {

    private Long id;
    private String saleNumber;
    private SaleStatus status;
    private Instant createdAt;
    private Instant cancelledAt;

    private String sellerName;
    private Long sellerId;
    private String cashierName;
    private Long cashierId;

    private String customerName;
    private Long customerId;

    private SaleCancellationReason cancellationReason;
    private String cancellationReasonLabel;
    private String cancellationComment;

    private BigDecimal total;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;

    private List<SaleLineResponse> lignes;

    private CancelledSalePaymentInfo paymentInfo;

    private List<SaleEventResponse> timeline;

    private CancelledSaleAuditInfo audit;
}
