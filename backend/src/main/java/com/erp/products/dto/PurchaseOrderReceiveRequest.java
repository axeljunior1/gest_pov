package com.erp.products.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderReceiveRequest {

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long locationId;

    private boolean validateEntry = true;

    @NotEmpty
    @Valid
    private List<LineReceive> lines;

    @Data
    public static class LineReceive {
        @NotNull
        private Long lineId;

        @NotNull
        @Positive
        private BigDecimal quantity;
    }
}
