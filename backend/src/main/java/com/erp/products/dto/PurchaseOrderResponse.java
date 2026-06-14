package com.erp.products.dto;

import com.erp.products.domain.enums.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String reference;
    private Long supplierId;
    private String supplierNom;
    private Long warehouseId;
    private String warehouseCode;
    private Long stockEntryId;
    private String stockEntryNumber;
    private LocalDate expectedDeliveryDate;
    private PurchaseOrderStatus status;
    private String notes;
    private List<LineResponse> lines;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class LineResponse {
        private Long id;
        private Long productId;
        private String productNom;
        private String productSku;
        private Long variantId;
        private String variantLabel;
        private BigDecimal quantity;
        private BigDecimal receivedQuantity;
        private BigDecimal remainingQuantity;
        private BigDecimal unitPrice;
        private String notes;
    }
}
