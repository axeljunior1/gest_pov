package com.erp.products.dto;

import com.erp.products.domain.enums.PosSessionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSessionOpenRequest {

    private Long warehouseId;
    private BigDecimal openingCashAmount;
    private PosSessionType sessionType;
}
