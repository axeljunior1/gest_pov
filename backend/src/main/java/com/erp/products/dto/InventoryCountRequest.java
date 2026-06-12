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
public class InventoryCountRequest {
    @NotBlank
    private String reference;
    @NotNull
    private Long warehouseId;
    private String utilisateur = "system";
    @NotEmpty
    @Valid
    private List<Line> lignes;

    @Data
    public static class Line {
        @NotNull
        private Long productId;
        private Long variantId;
        @NotNull
        private Long locationId;
        private Long lotId;
        @NotNull
        @Positive
        private BigDecimal quantityCounted;
    }
}
