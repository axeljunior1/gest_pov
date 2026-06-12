package com.erp.products.dto;

import com.erp.products.domain.enums.StockMovementType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockMovementResponse {
    private Long id;
    private StockMovementType movementType;
    private Long productId;
    private String productNom;
    private Long variantId;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private Long lotId;
    private String lotNumero;
    private String unitSymbole;
    private BigDecimal quantity;
    private BigDecimal quantityOnHandBefore;
    private BigDecimal quantityOnHandAfter;
    private BigDecimal quantityReservedBefore;
    private BigDecimal quantityReservedAfter;
    private String referenceType;
    private String reference;
    private String reason;
    private String utilisateur;
    private Instant movementDate;
}
