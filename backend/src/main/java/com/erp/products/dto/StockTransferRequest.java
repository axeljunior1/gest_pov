package com.erp.products.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockTransferRequest {
    @NotBlank
    private String reference;
    @NotNull
    private Long sourceWarehouseId;
    @NotNull
    private Long destWarehouseId;
    private String notes;
    private String utilisateur = "system";
    @NotEmpty
    @Valid
    private List<Line> lignes;

    @Data
    public static class Line {
        @NotNull
        private Long productId;
        private Long variantId;
        private Long lotId;
        @NotNull
        @Positive
        private BigDecimal quantity;
        @NotNull
        private Long sourceLocationId;
        @NotNull
        private Long destLocationId;
    }
}
