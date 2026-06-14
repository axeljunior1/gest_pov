package com.erp.products.dto;

import com.erp.products.domain.enums.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderRequest {

    @NotNull
    private Long supplierId;

    private Long warehouseId;

    @NotNull
    private LocalDate expectedDeliveryDate;

    private String notes;

    @NotEmpty
    @Valid
    private List<LineRequest> lines;

    @Data
    public static class LineRequest {
        @NotNull
        private Long productId;
        private Long variantId;
        @NotNull
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String notes;
    }
}
