package com.erp.products.dto;

import com.erp.products.domain.enums.StockEntryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockEntryRequest {

    private Long supplierId;

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long locationId;

    private LocalDate entryDate;

    private String referenceDocument;

    private String notes;

    private String createdBy;

    @NotEmpty
    @Valid
    private List<Line> lignes;

    @Data
    public static class Line {
        private Long id;

        @NotNull
        private Long productId;

        private Long variantId;

        private Long packagingId;

        @NotNull
        @Positive
        private BigDecimal quantityInput;

        private BigDecimal unitCost;

        private String lotNumber;

        private LocalDate expiryDate;

        private String notes;
    }
}
