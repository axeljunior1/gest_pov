package com.erp.products.service;

import com.erp.products.config.StockProperties;
import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.*;
import com.erp.products.service.alert.AlertRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Moteur interne : toute modification de stock passe par ici + enregistrement d'un mouvement.
 */
@Service
@RequiredArgsConstructor
class StockLedgerService {

    private final StockItemRepository stockItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final StockProperties stockProperties;
    private final AlertRuleEngine alertRuleEngine;

    record MovementMeta(
            StockMovementType type,
            String referenceType,
            String reference,
            String reason,
            String utilisateur,
            Long stockTransferId,
            Long stockReservationId,
            Long inventoryCountId,
            Long stockEntryId
    ) {}

    @Transactional
    public StockMovement applyOnHandChange(
            Long productId,
            Long variantId,
            Long warehouseId,
            Long locationId,
            Long lotId,
            BigDecimal delta,
            MovementMeta meta) {

        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("La quantité de mouvement ne peut pas être nulle");
        }

        ResolvedPosition pos = resolvePosition(productId, variantId, warehouseId, locationId, lotId);
        StockItem item = getOrCreateForUpdate(pos);
        BigDecimal beforeOnHand = item.getQuantityOnHand();
        BigDecimal beforeReserved = item.getQuantityReserved();
        BigDecimal afterOnHand = beforeOnHand.add(delta);

        if (!stockProperties.isAllowNegativeStock() && afterOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Stock insuffisant — stock physique ne peut pas devenir négatif");
        }

        item.setQuantityOnHand(afterOnHand.setScale(6, RoundingMode.HALF_UP));
        stockItemRepository.save(item);

        StockMovement movement = recordMovement(pos, item, meta,
                delta.abs(), beforeOnHand, item.getQuantityOnHand(),
                beforeReserved, item.getQuantityReserved());

        syncVariantStock(pos.variant());
        alertRuleEngine.afterStockMovement(
                productId,
                variantId,
                warehouseId,
                locationId,
                lotId);
        return movement;
    }

    @Transactional
    public StockMovement applyReservedChange(
            Long productId,
            Long variantId,
            Long warehouseId,
            Long locationId,
            Long lotId,
            BigDecimal reservedDelta,
            MovementMeta meta) {

        if (reservedDelta == null || reservedDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("La quantité réservée ne peut pas être nulle");
        }

        ResolvedPosition pos = resolvePosition(productId, variantId, warehouseId, locationId, lotId);
        StockItem item = getOrCreateForUpdate(pos);
        BigDecimal beforeOnHand = item.getQuantityOnHand();
        BigDecimal beforeReserved = item.getQuantityReserved();
        BigDecimal afterReserved = beforeReserved.add(reservedDelta);

        if (afterReserved.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Impossible de libérer plus que le stock réservé");
        }
        if (afterReserved.compareTo(beforeOnHand) > 0) {
            throw new BusinessException("Stock disponible insuffisant pour la réservation");
        }

        item.setQuantityReserved(afterReserved.setScale(6, RoundingMode.HALF_UP));
        stockItemRepository.save(item);

        StockMovement movement = recordMovement(pos, item, meta,
                reservedDelta.abs(), beforeOnHand, item.getQuantityOnHand(),
                beforeReserved, item.getQuantityReserved());

        alertRuleEngine.afterStockMovement(
                productId, variantId, warehouseId, locationId, lotId);
        return movement;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailable(Long productId, Long variantId, Long warehouseId) {
        if (warehouseId != null) {
            return stockItemRepository.findByWarehouseId(warehouseId).stream()
                    .filter(i -> i.getProduct().getId().equals(productId))
                    .filter(i -> variantId == null
                            ? i.getVariant() == null
                            : i.getVariant() != null && i.getVariant().getId().equals(variantId))
                    .map(StockItem::getQuantityAvailable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return stockItemRepository.sumAvailableByProductAndVariant(productId, variantId);
    }

    private StockItem getOrCreateForUpdate(ResolvedPosition pos) {
        Long lotKey = pos.lot() != null ? pos.lot().getId() : 0L;
        return stockItemRepository.findForUpdate(
                        pos.product().getId(),
                        pos.variant() != null ? pos.variant().getId() : null,
                        pos.warehouse().getId(),
                        pos.location().getId(),
                        lotKey)
                .orElseGet(() -> createStockItem(pos, lotKey));
    }

    private StockItem createStockItem(ResolvedPosition pos, Long lotKey) {
        StockItem item = StockItem.builder()
                .product(pos.product())
                .variant(pos.variant())
                .warehouse(pos.warehouse())
                .location(pos.location())
                .lot(pos.lot())
                .lotKey(lotKey)
                .unit(pos.product().getUnit())
                .quantityOnHand(BigDecimal.ZERO)
                .quantityReserved(BigDecimal.ZERO)
                .build();
        return stockItemRepository.save(item);
    }

    private StockMovement recordMovement(
            ResolvedPosition pos,
            StockItem item,
            MovementMeta meta,
            BigDecimal quantityAbs,
            BigDecimal onHandBefore,
            BigDecimal onHandAfter,
            BigDecimal reservedBefore,
            BigDecimal reservedAfter) {

        StockMovement movement = StockMovement.builder()
                .movementType(meta.type())
                .product(pos.product())
                .variant(pos.variant())
                .warehouse(pos.warehouse())
                .location(pos.location())
                .lot(pos.lot())
                .unit(pos.product().getUnit())
                .quantity(quantityAbs.setScale(6, RoundingMode.HALF_UP))
                .quantityOnHandBefore(onHandBefore)
                .quantityOnHandAfter(onHandAfter)
                .quantityReservedBefore(reservedBefore)
                .quantityReservedAfter(reservedAfter)
                .referenceType(meta.referenceType())
                .reference(meta.reference())
                .reason(meta.reason())
                .utilisateur(meta.utilisateur() != null ? meta.utilisateur() : "system")
                .movementDate(Instant.now())
                .stockTransferId(meta.stockTransferId())
                .stockReservationId(meta.stockReservationId())
                .inventoryCountId(meta.inventoryCountId())
                .stockEntryId(meta.stockEntryId())
                .build();
        return stockMovementRepository.save(movement);
    }

    private void syncVariantStock(ProductVariant variant) {
        if (variant == null) {
            return;
        }
        BigDecimal total = stockItemRepository.sumQuantityOnHandByVariantId(variant.getId());
        variant.setStock(total.setScale(0, RoundingMode.HALF_UP).intValue());
        variantRepository.save(variant);
    }

    private ResolvedPosition resolvePosition(
            Long productId, Long variantId, Long warehouseId, Long locationId, Long lotId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + productId));
        if (product.getUnit() == null) {
            throw new BusinessException("Le produit n'a pas d'unité de base définie");
        }

        ProductVariant variant = null;
        if (variantId != null) {
            variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée: " + variantId));
            if (!variant.getProduct().getId().equals(productId)) {
                throw new BusinessException("Variante invalide pour ce produit");
            }
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé: " + warehouseId));
        if (!Boolean.TRUE.equals(warehouse.getActif())) {
            throw new BusinessException("Entrepôt inactif: " + warehouse.getCode());
        }

        Location location = locationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Emplacement non trouvé pour cet entrepôt"));
        if (!Boolean.TRUE.equals(location.getActif())) {
            throw new BusinessException("Emplacement inactif: " + location.getCode());
        }

        Lot lot = null;
        if (lotId != null) {
            lot = lotRepository.findById(lotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lot non trouvé: " + lotId));
        }

        return new ResolvedPosition(product, variant, warehouse, location, lot);
    }

    private record ResolvedPosition(
            Product product,
            ProductVariant variant,
            Warehouse warehouse,
            Location location,
            Lot lot) {}
}
