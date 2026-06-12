package com.erp.products.dto;

import com.erp.products.domain.enums.InventoryCountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class InventoryCountResponse {
    private Long id;
    private String inventoryNumber;
    private String reference;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private InventoryCountStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String notes;
    private String createdBy;
    private Instant createdAt;
    private String validatedBy;
    private Instant validatedAt;
    private String cancelledBy;
    private Instant cancelledAt;
    private List<Line> lignes;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long productId;
        private String productNom;
        private Long variantId;
        private Long locationId;
        private String locationCode;
        private Long lotId;
        private String lotNumero;
        private Long packagingId;
        private String packagingNom;
        private BigDecimal quantitySystem;
        private BigDecimal quantityInput;
        private BigDecimal quantityCounted;
        private BigDecimal differenceQuantity;
        private String notes;
    }
}
