package com.erp.products.dto;

import com.erp.products.domain.enums.PosSessionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PosSessionResponse {

    private Long id;
    private String sessionNumber;
    private Long cashierId;
    private String cashierName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseNom;
    private BigDecimal openingCashAmount;
    private BigDecimal closingCashAmount;
    private BigDecimal expectedCashAmount;
    private BigDecimal differenceAmount;
    private PosSessionStatus status;
    private Instant openedAt;
    private Instant closedAt;
}
