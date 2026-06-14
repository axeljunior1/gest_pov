package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.StockValuationMethod;
import com.erp.products.repository.StockItemRepository;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockValuationService {

    private final StockItemRepository stockItemRepository;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public StockValuationMethod getMethod() {
        String raw = settingsService.getSetting(SettingKeys.STOCK_VALUATION_METHOD);
        if (raw != null && "SALE_PRICE".equalsIgnoreCase(raw.trim())) {
            return StockValuationMethod.SALE_PRICE;
        }
        return StockValuationMethod.PURCHASE_COST;
    }

    @Transactional(readOnly = true)
    public BigDecimal computeTotalStockValue() {
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
