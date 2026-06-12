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
    private Long unitId;
    private String unitSymbole;
    private Long packagingId;
    private String packagingNom;
    private BigDecimal quantity;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private BigDecimal quantityOnHandBefore;
    private BigDecimal quantityOnHandAfter;
    private BigDecimal quantityReservedBefore;
    private BigDecimal quantityReservedAfter;
    private String referenceType;
    private Long referenceId;
    private String reference;
    private String reason;
    private String notes;
    private String createdBy;
    private String utilisateur;
    private Instant movementDate;
    private Instant createdAt;
    private Long stockEntryId;
    private Long stockExitId;
    private Long stockTransferId;
    private Long stockReservationId;
    private Long inventoryCountId;
}
