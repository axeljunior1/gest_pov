package com.erp.products.dto;

import com.erp.products.domain.enums.StockTransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StockTransferResponse {
    private Long id;
    private String reference;
    private Long sourceWarehouseId;
    private String sourceWarehouseCode;
    private Long destWarehouseId;
    private String destWarehouseCode;
    private StockTransferStatus status;
    private String notes;
    private String utilisateur;
    private Instant createdAt;
    private Instant shippedAt;
    private Instant receivedAt;
    private List<Line> lignes;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long productId;
        private Long variantId;
        private Long lotId;
        private BigDecimal quantity;
        private Long sourceLocationId;
        private Long destLocationId;
    }
}
