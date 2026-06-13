package com.erp.products.dto;

import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StockExitResponse {

    private Long id;
    private String exitNumber;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private LocalDate exitDate;
    private StockExitReason reason;
    private String notes;
    private StockExitStatus status;
    private String createdBy;
    private Instant createdAt;
    private String validatedBy;
    private Instant validatedAt;
    private String cancelledBy;
    private Instant cancelledAt;
    private Long saleId;
    private String saleNumber;
    private boolean posOrigin;
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
        private String notes;
    }
}
