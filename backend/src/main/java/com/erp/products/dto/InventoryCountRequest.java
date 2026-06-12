package com.erp.products.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InventoryCountRequest {
    @NotNull
    private Long warehouseId;
    private Long locationId;
    private String notes;
    private String createdBy;
    @NotEmpty
    @Valid
    private List<Line> lignes;

    @Data
    public static class Line {
        @NotNull
        private Long productId;
        private Long variantId;
        private Long locationId;
        private Long lotId;
        private Long packagingId;
        @NotNull
        @Positive
        private BigDecimal quantityInput;
        private String notes;
    }
}
