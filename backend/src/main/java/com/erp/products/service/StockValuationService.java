package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.StockValuationMethod;
import com.erp.products.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockValuationService {

    private final StockItemRepository stockItemRepository;
    private final SettingsService settingsService;
    private final com.erp.products.service.stockvaluation.StockCmpValuationService cmpValuationService;

    @Transactional(readOnly = true)
    public StockValuationMethod getMethod() {
        return settingsService.getStockConfig().getValuationMethod();
    }

    @Transactional(readOnly = true)
    public BigDecimal computeTotalStockValue() {
        if (getMethod() == StockValuationMethod.WEIGHTED_AVERAGE) {
            return cmpValuationService.getTotalStockValue();
        }
        return stockItemRepository.sumEligibleStockValue(usesSalePrice());
    }

    @Transactional(readOnly = true)
    public boolean usesSalePrice() {
        return getMethod() == StockValuationMethod.SALE_PRICE;
    }

    /**
     * Prix unitaire de valorisation pour une ligne de stock (variante ou produit simple).
     */
    public BigDecimal resolveUnitValuationPrice(Product product, ProductVariant variant, StockValuationMethod method) {
        if (method == StockValuationMethod.WEIGHTED_AVERAGE) {
            Long variantId = variant != null ? variant.getId() : null;
            BigDecimal avg = cmpValuationService.getCurrentAverageCost(product.getId(), variantId);
            if (avg.compareTo(BigDecimal.ZERO) > 0) {
                return avg;
            }
            return cmpValuationService.resolveFallbackCost(product.getId(), variantId);
        }
        if (method == StockValuationMethod.SALE_PRICE) {
            if (variant != null && variant.getPrix() != null && variant.getPrix().compareTo(BigDecimal.ZERO) > 0) {
                return variant.getPrix();
            }
            return positiveOrNull(product.getPrixVente());
        }
        if (variant != null && variant.getCostPrice() != null && variant.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getCostPrice();
        }
        return positiveOrNull(product.getPrixAchat());
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value;
    }
}
