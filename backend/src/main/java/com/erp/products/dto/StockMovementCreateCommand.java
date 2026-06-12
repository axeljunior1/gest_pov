package com.erp.products.dto;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.StockMovementType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record StockMovementCreateCommand(
        StockMovementType movementType,
        Product product,
        ProductVariant variant,
        Warehouse warehouse,
        Location location,
        Lot lot,
        UnitOfMeasure unit,
        BigDecimal quantity,
        BigDecimal quantityOnHandBefore,
        BigDecimal quantityOnHandAfter,
        BigDecimal quantityReservedBefore,
        BigDecimal quantityReservedAfter,
        String referenceType,
        String reference,
        Long referenceId,
        String reason,
        String notes,
        String createdBy,
        Long packagingId,
        Long stockTransferId,
        Long stockReservationId,
        Long inventoryCountId,
        Long stockEntryId,
        Long stockExitId) {
}
