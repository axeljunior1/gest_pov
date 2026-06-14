package com.erp.products.dto;

import com.erp.products.domain.enums.AlertSettingScope;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AlertSettingResponse {
    private Long id;
    private AlertSettingScope scope;
    private Long productId;
    private String productNom;
    private String productSku;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseNom;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private Integer expiryAlertDays;
    private Integer dormantDays;
    private Boolean actif;
    private Instant createdAt;
    private Instant updatedAt;
}
