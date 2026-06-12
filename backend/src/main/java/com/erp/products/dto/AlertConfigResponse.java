package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AlertConfigResponse {

    private BigDecimal minStockLevelDefault;
    private Integer expiryAlertDaysDefault;
}
