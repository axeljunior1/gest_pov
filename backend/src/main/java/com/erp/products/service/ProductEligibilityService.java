package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * Règles communes : produit vendable / comptabilisé en stock.
 */
@Service
public class ProductEligibilityService {

    public static final String MSG_PRODUCT_INACTIVE = "Ce produit n'est pas actif.";
    public static final String MSG_PRODUCT_LIFECYCLE = "Ce produit n'est pas disponible à la vente (cycle de vie).";

    public boolean isCommerciallyActive(Product product) {
        return product != null
                && product.getStatut() == ProductStatus.ACTIF
                && product.getCycleVie() == LifecycleStatus.ACTIF;
    }

    /** Stock physique compté dans les indicateurs (dashboard, alertes). */
    public boolean countsInStockMetrics(Product product) {
        return isCommerciallyActive(product);
    }

    public void assertCommerciallyActive(Product product) {
        if (product == null) {
            throw new BusinessException("Produit introuvable.");
        }
        if (product.getStatut() != ProductStatus.ACTIF) {
            throw new BusinessException(MSG_PRODUCT_INACTIVE);
        }
        if (product.getCycleVie() != LifecycleStatus.ACTIF) {
            throw new BusinessException(MSG_PRODUCT_LIFECYCLE);
        }
    }
}
