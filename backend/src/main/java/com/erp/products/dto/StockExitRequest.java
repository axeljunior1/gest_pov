package com.erp.products.dto;

import com.erp.products.domain.enums.StockExitReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockExitRequest {

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long locationId;

    private LocalDate exitDate;

    @NotNull
    private StockExitReason reason;

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

        private String notes;
    }
}
