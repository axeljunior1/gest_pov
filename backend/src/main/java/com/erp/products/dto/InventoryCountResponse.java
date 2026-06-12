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
    private String reference;
    private Long warehouseId;
    private String warehouseCode;
    private InventoryCountStatus status;
    private String utilisateur;
    private Instant createdAt;
    private Instant validatedAt;
    private List<Line> lignes;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long productId;
        private Long variantId;
        private Long locationId;
        private Long lotId;
        private BigDecimal quantitySystem;
        private BigDecimal quantityCounted;
        private BigDecimal ecart;
    }
}
