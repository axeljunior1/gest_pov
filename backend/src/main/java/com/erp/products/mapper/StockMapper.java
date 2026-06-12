package com.erp.products.mapper;

import com.erp.products.domain.entity.*;
import com.erp.products.dto.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StockMapper {

    public WarehouseResponse toWarehouseResponse(Warehouse w) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .code(w.getCode())
                .nom(w.getNom())
                .adresse(w.getAdresse())
                .actif(w.getActif())
                .createdAt(w.getCreatedAt())
                .build();
    }

    public LocationResponse toLocationResponse(Location l) {
        return LocationResponse.builder()
                .id(l.getId())
                .warehouseId(l.getWarehouse().getId())
                .warehouseCode(l.getWarehouse().getCode())
                .code(l.getCode())
                .nom(l.getNom())
                .actif(l.getActif())
                .build();
    }

    public LotResponse toLotResponse(Lot lot) {
        return LotResponse.builder()
                .id(lot.getId())
                .productId(lot.getProduct().getId())
                .variantId(lot.getVariant() != null ? lot.getVariant().getId() : null)
                .numeroLot(lot.getNumeroLot())
                .datePeremption(lot.getDatePeremption())
                .dateFabrication(lot.getDateFabrication())
                .build();
    }

    public StockItemResponse toStockItemResponse(StockItem item) {
        return StockItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productNom(item.getProduct().getNom())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .warehouseId(item.getWarehouse().getId())
                .warehouseCode(item.getWarehouse().getCode())
                .locationId(item.getLocation().getId())
                .locationCode(item.getLocation().getCode())
                .lotId(item.getLot() != null ? item.getLot().getId() : null)
                .lotNumero(item.getLot() != null ? item.getLot().getNumeroLot() : null)
                .unitSymbole(item.getUnit().getSymbole())
                .quantityOnHand(item.getQuantityOnHand())
                .quantityReserved(item.getQuantityReserved())
                .quantityAvailable(item.getQuantityAvailable())
                .build();
    }

    public StockMovementResponse toMovementResponse(StockMovement m) {
        return StockMovementResponse.builder()
                .id(m.getId())
                .movementType(m.getMovementType())
                .productId(m.getProduct().getId())
                .productNom(m.getProduct().getNom())
                .variantId(m.getVariant() != null ? m.getVariant().getId() : null)
                .warehouseId(m.getWarehouse().getId())
                .warehouseCode(m.getWarehouse().getCode())
                .locationId(m.getLocation().getId())
                .locationCode(m.getLocation().getCode())
                .lotId(m.getLot() != null ? m.getLot().getId() : null)
                .lotNumero(m.getLot() != null ? m.getLot().getNumeroLot() : null)
                .unitId(m.getUnit().getId())
                .unitSymbole(m.getUnit().getSymbole())
                .packagingId(m.getPackaging() != null ? m.getPackaging().getId() : null)
                .packagingNom(m.getPackaging() != null ? m.getPackaging().getNom() : null)
                .quantity(m.getQuantity())
                .quantityBefore(m.getQuantityOnHandBefore())
                .quantityAfter(m.getQuantityOnHandAfter())
                .quantityOnHandBefore(m.getQuantityOnHandBefore())
                .quantityOnHandAfter(m.getQuantityOnHandAfter())
                .quantityReservedBefore(m.getQuantityReservedBefore())
                .quantityReservedAfter(m.getQuantityReservedAfter())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .reference(m.getReference())
                .reason(m.getReason())
                .notes(m.getNotes())
                .createdBy(m.getUtilisateur())
                .utilisateur(m.getUtilisateur())
                .movementDate(m.getMovementDate())
                .createdAt(m.getCreatedAt())
                .stockEntryId(m.getStockEntryId())
                .stockExitId(m.getStockExitId())
                .stockTransferId(m.getStockTransferId())
                .stockReservationId(m.getStockReservationId())
                .inventoryCountId(m.getInventoryCountId())
                .build();
    }

    public StockReservationResponse toReservationResponse(StockReservation r) {
        return StockReservationResponse.builder()
                .id(r.getId())
                .productId(r.getProduct().getId())
                .variantId(r.getVariant() != null ? r.getVariant().getId() : null)
                .warehouseId(r.getWarehouse().getId())
                .locationId(r.getLocation().getId())
                .lotId(r.getLot() != null ? r.getLot().getId() : null)
                .quantity(r.getQuantity())
                .status(r.getStatus())
                .reference(r.getReference())
                .utilisateur(r.getUtilisateur())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public StockTransferResponse toTransferResponse(StockTransfer t) {
        return StockTransferResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .sourceWarehouseId(t.getSourceWarehouse().getId())
                .sourceWarehouseCode(t.getSourceWarehouse().getCode())
                .destWarehouseId(t.getDestWarehouse().getId())
                .destWarehouseCode(t.getDestWarehouse().getCode())
                .status(t.getStatus())
                .notes(t.getNotes())
                .utilisateur(t.getUtilisateur())
                .createdAt(t.getCreatedAt())
                .shippedAt(t.getShippedAt())
                .receivedAt(t.getReceivedAt())
                .lignes(t.getLignes().stream().map(l -> StockTransferResponse.Line.builder()
                        .id(l.getId())
                        .productId(l.getProduct().getId())
                        .variantId(l.getVariant() != null ? l.getVariant().getId() : null)
                        .lotId(l.getLot() != null ? l.getLot().getId() : null)
                        .quantity(l.getQuantity())
                        .sourceLocationId(l.getSourceLocation().getId())
                        .destLocationId(l.getDestLocation().getId())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    public InventoryCountResponse toInventoryResponse(InventoryCount c) {
        return InventoryCountResponse.builder()
                .id(c.getId())
                .inventoryNumber(c.getInventoryNumber())
                .reference(c.getReference())
                .warehouseId(c.getWarehouse().getId())
                .warehouseCode(c.getWarehouse().getCode())
                .locationId(c.getLocation() != null ? c.getLocation().getId() : null)
                .locationCode(c.getLocation() != null ? c.getLocation().getCode() : null)
                .status(c.getStatus())
                .startedAt(c.getStartedAt())
                .completedAt(c.getCompletedAt())
                .notes(c.getNotes())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .validatedBy(c.getValidatedBy())
                .validatedAt(c.getValidatedAt())
                .cancelledBy(c.getCancelledBy())
                .cancelledAt(c.getCancelledAt())
                .lignes(c.getLignes().stream().map(l -> InventoryCountResponse.Line.builder()
                        .id(l.getId())
                        .productId(l.getProduct().getId())
                        .productNom(l.getProduct().getNom())
                        .variantId(l.getVariant() != null ? l.getVariant().getId() : null)
                        .locationId(l.getLocation().getId())
                        .locationCode(l.getLocation().getCode())
                        .lotId(l.getLot() != null ? l.getLot().getId() : null)
                        .lotNumero(l.getLot() != null ? l.getLot().getNumeroLot() : null)
                        .packagingId(l.getPackaging() != null ? l.getPackaging().getId() : null)
                        .packagingNom(l.getPackaging() != null ? l.getPackaging().getNom() : null)
                        .quantitySystem(l.getQuantitySystem())
                        .quantityInput(l.getQuantityInput())
                        .quantityCounted(l.getQuantityCounted())
                        .differenceQuantity(l.getEcart())
                        .notes(l.getNotes())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    public StockEntryResponse toEntryResponse(StockEntry e) {
        return StockEntryResponse.builder()
                .id(e.getId())
                .entryNumber(e.getEntryNumber())
                .supplierId(e.getSupplier() != null ? e.getSupplier().getId() : null)
                .supplierNom(e.getSupplier() != null ? e.getSupplier().getNom() : null)
                .warehouseId(e.getWarehouse().getId())
                .warehouseCode(e.getWarehouse().getCode())
                .locationId(e.getLocation().getId())
                .locationCode(e.getLocation().getCode())
                .entryDate(e.getEntryDate())
                .referenceDocument(e.getReferenceDocument())
                .notes(e.getNotes())
                .status(e.getStatus())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .validatedBy(e.getValidatedBy())
                .validatedAt(e.getValidatedAt())
                .cancelledBy(e.getCancelledBy())
                .cancelledAt(e.getCancelledAt())
                .lignes(e.getLignes().stream().map(l -> StockEntryResponse.Line.builder()
                        .id(l.getId())
                        .productId(l.getProduct().getId())
                        .productNom(l.getProduct().getNom())
                        .variantId(l.getVariant() != null ? l.getVariant().getId() : null)
                        .packagingId(l.getPackaging() != null ? l.getPackaging().getId() : null)
                        .packagingNom(l.getPackaging() != null ? l.getPackaging().getNom() : null)
                        .quantityInput(l.getQuantityInput())
                        .quantityInBaseUnit(l.getQuantityInBaseUnit())
                        .unitSymbole(l.getProduct().getUnit() != null ? l.getProduct().getUnit().getSymbole() : null)
                        .unitCost(l.getUnitCost())
                        .lotNumber(l.getLotNumber())
                        .expiryDate(l.getExpiryDate())
                        .notes(l.getNotes())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    public StockExitResponse toExitResponse(StockExit e) {
        return StockExitResponse.builder()
                .id(e.getId())
                .exitNumber(e.getExitNumber())
                .warehouseId(e.getWarehouse().getId())
                .warehouseCode(e.getWarehouse().getCode())
                .locationId(e.getLocation().getId())
                .locationCode(e.getLocation().getCode())
                .exitDate(e.getExitDate())
                .reason(e.getReason())
                .notes(e.getNotes())
                .status(e.getStatus())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .validatedBy(e.getValidatedBy())
                .validatedAt(e.getValidatedAt())
                .cancelledBy(e.getCancelledBy())
                .cancelledAt(e.getCancelledAt())
                .lignes(e.getLignes().stream().map(l -> StockExitResponse.Line.builder()
                        .id(l.getId())
                        .productId(l.getProduct().getId())
                        .productNom(l.getProduct().getNom())
                        .variantId(l.getVariant() != null ? l.getVariant().getId() : null)
                        .packagingId(l.getPackaging() != null ? l.getPackaging().getId() : null)
                        .packagingNom(l.getPackaging() != null ? l.getPackaging().getNom() : null)
                        .quantityInput(l.getQuantityInput())
                        .quantityInBaseUnit(l.getQuantityInBaseUnit())
                        .unitSymbole(l.getProduct().getUnit() != null ? l.getProduct().getUnit().getSymbole() : null)
                        .notes(l.getNotes())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
