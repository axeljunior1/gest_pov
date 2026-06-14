package com.erp.products.dto;

import com.erp.products.domain.enums.AlertSettingScope;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertSettingRequest {

    @NotNull
    private AlertSettingScope scope;

    private Long productId;
    private Long warehouseId;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private Integer expiryAlertDays;
    private Integer dormantDays;
    private Boolean actif;
}
