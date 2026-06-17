package com.erp.products.service.stockvaluation;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.entity.StockValuation;
import com.erp.products.domain.entity.StockValuationMovement;
import com.erp.products.domain.enums.StockValuationMovementType;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import com.erp.products.repository.StockValuationMovementRepository;
import com.erp.products.repository.StockValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class StockCmpValuationService {

    private final StockValuationRepository valuationRepository;
    private final StockValuationMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public BigDecimal recordPurchase(
            Long productId,
            Long variantId,
            BigDecimal quantity,
            BigDecimal unitCost,
            Instant movementDate,
            Long sourceId,
            String sourceType) {
        CmpCalculator.CmpMovementResult result = applyMovement(
                productId, variantId,
                CmpCalculator.applyInbound(loadState(productId, variantId), quantity, unitCost),
                StockValuationMovementType.PURCHASE,
                movementDate, sourceId, sourceType);
        return result.unitCostApplied();
    }

    @Transactional
    public BigDecimal recordSale(
            Long productId,
            Long variantId,
            BigDecimal quantity,
            Instant movementDate,
            Long sourceId,
            String sourceType) {
        CmpCalculator.CmpMovementResult result = applyMovement(
                productId, variantId,
                CmpCalculator.applyOutbound(loadState(productId, variantId), quantity),
                StockValuationMovementType.SALE,
                movementDate, sourceId, sourceType);
        return result.unitCostApplied();
    }

    @Transactional
    public void recordReturn(
            Long productId,
            Long variantId,
            BigDecimal quantity,
            BigDecimal unitCostAtOriginalSale,
            Instant movementDate,
            Long sourceId,
            String sourceType) {
        applyMovement(
                productId, variantId,
                CmpCalculator.applyInbound(loadState(productId, variantId), quantity, unitCostAtOriginalSale),
                StockValuationMovementType.RETURN,
                movementDate, sourceId, sourceType);
    }

    @Transactional
    public void recordPurchaseReversal(
            Long productId,
            Long variantId,
            BigDecimal quantity,
            BigDecimal originalUnitCost,
            Instant movementDate,
            Long sourceId,
            String sourceType) {
        applyMovement(
                productId, variantId,
                CmpCalculator.applyPurchaseReversal(loadState(productId, variantId), quantity, originalUnitCost),
                StockValuationMovementType.ADJUSTMENT,
                movementDate, sourceId, sourceType);
    }

    @Transactional
    public void recordInventoryAdjustment(
            Long productId,
            Long variantId,
            BigDecimal quantityDelta,
            Instant movementDate,
            Long sourceId,
            String sourceType) {
        if (quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        CmpCalculator.CmpState state = loadState(productId, variantId);
        CmpCalculator.CmpMovementResult calc;
        StockValuationMovementType type;
        if (quantityDelta.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unitCost = state.averageUnitCost().compareTo(BigDecimal.ZERO) > 0
                    ? state.averageUnitCost()
                    : resolveFallbackCost(productId, variantId);
            calc = CmpCalculator.applyInbound(state, quantityDelta, unitCost);
            type = StockValuationMovementType.INVENTORY;
        } else {
            calc = CmpCalculator.applyOutbound(state, quantityDelta.abs());
            type = StockValuationMovementType.INVENTORY;
        }
        applyMovement(productId, variantId, calc, type, movementDate, sourceId, sourceType);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentAverageCost(Long productId, Long variantId) {
        return valuationRepository.findByProductAndVariant(productId, variantId)
                .map(StockValuation::getAverageUnitCost)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentStockValue(Long productId, Long variantId) {
        return valuationRepository.findByProductAndVariant(productId, variantId)
                .map(StockValuation::getStockValue)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalStockValue() {
        return valuationRepository.sumTotalStockValue();
    }

    public BigDecimal resolveFallbackCost(Long productId, Long variantId) {
        if (variantId != null) {
            ProductVariant variant = variantRepository.findById(variantId).orElse(null);
            if (variant != null && variant.getCostPrice() != null
                    && variant.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                return variant.getCostPrice();
            }
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Produit introuvable: " + productId));
        if (product.getPrixAchat() != null && product.getPrixAchat().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPrixAchat();
        }
        return BigDecimal.ZERO;
    }

    public Instant toInstant(LocalDate date) {
        if (date == null) {
            return Instant.now();
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveOutboundUnitCostFromSource(
            Long sourceId,
            String sourceType,
            Long productId,
            Long variantId) {
        return movementRepository.findLatestOutboundBySource(sourceId, sourceType, productId, variantId)
                .map(StockValuationMovement::getUnitCost)
                .orElseGet(() -> resolveFallbackCost(productId, variantId));
    }

    private CmpCalculator.CmpState loadState(Long productId, Long variantId) {
        return valuationRepository.findForUpdate(productId, variantId)
                .map(v -> new CmpCalculator.CmpState(
                        v.getQuantityOnHand(), v.getStockValue(), v.getAverageUnitCost()))
                .orElse(CmpCalculator.CmpState.zero());
    }

    private CmpCalculator.CmpMovementResult applyMovement(
            Long productId,
            Long variantId,
            CmpCalculator.CmpMovementResult calc,
            StockValuationMovementType type,
            Instant movementDate,
            Long sourceId,
            String sourceType) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Produit introuvable: " + productId));
        ProductVariant variant = variantId != null
                ? variantRepository.findById(variantId).orElse(null)
                : null;

        StockValuation valuation = valuationRepository.findForUpdate(productId, variantId)
                .orElseGet(() -> StockValuation.builder()
                        .product(product)
                        .variant(variant)
                        .quantityOnHand(BigDecimal.ZERO)
                        .stockValue(BigDecimal.ZERO)
                        .averageUnitCost(BigDecimal.ZERO)
                        .build());

        CmpCalculator.CmpState after = calc.stateAfter();
        valuation.setQuantityOnHand(after.quantityOnHand());
        valuation.setStockValue(after.stockValue());
        valuation.setAverageUnitCost(after.averageUnitCost());
        valuationRepository.save(valuation);

        movementRepository.save(StockValuationMovement.builder()
                .product(product)
                .variant(variant)
                .movementType(type)
                .movementDate(movementDate != null ? movementDate : Instant.now())
                .quantity(calc.signedQuantity())
                .unitCost(calc.unitCostApplied())
                .totalValue(calc.totalValueMoved())
                .averageUnitCostAfter(after.averageUnitCost())
                .stockQuantityAfter(after.quantityOnHand())
                .stockValueAfter(after.stockValue())
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build());

        return calc;
    }
}
