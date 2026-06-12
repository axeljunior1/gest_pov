package com.erp.products.dto;

import com.erp.products.domain.enums.StockEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StockEntryResponse {

    private Long id;
    private String entryNumber;
    private Long supplierId;
    private String supplierNom;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private LocalDate entryDate;
    private String referenceDocument;
    private String notes;
    private StockEntryStatus status;
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
        private Long packagingId;
        private String packagingNom;
        private BigDecimal quantityInput;
        private BigDecimal quantityInBaseUnit;
        private String unitSymbole;
        private BigDecimal unitCost;
        private String lotNumber;
        private LocalDate expiryDate;
        private String notes;
    }
}
