package com.erp.products.dto;

import com.erp.products.domain.enums.StockValuationMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockConfigResponse {

    private boolean allowNegativeStock;
    private BigDecimal lowStockThresholdDefault;
    private StockValuationMethod valuationMethod;
    private boolean lowStockAlertsEnabled;
    private boolean multiWarehouseEnabled;
}
