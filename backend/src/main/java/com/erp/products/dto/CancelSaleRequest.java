package com.erp.products.dto;

import com.erp.products.domain.enums.SaleCancellationReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelSaleRequest {

    @NotNull
    private SaleCancellationReason reason;

    private String comment;
}
