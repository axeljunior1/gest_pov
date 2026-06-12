package com.erp.products.dto;

import com.erp.products.domain.enums.StockMovementType;
import lombok.Data;

import java.time.Instant;

@Data
public class StockMovementSearchCriteria {
    private Long productId;
    private Long warehouseId;
    private Long locationId;
    private StockMovementType movementType;
    private String referenceType;
    private Long referenceId;
    private String reference;
    private String createdBy;
    private Instant dateFrom;
    private Instant dateTo;
    private Integer limit = 500;
}
