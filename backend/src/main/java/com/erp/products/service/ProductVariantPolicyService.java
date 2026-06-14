package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import com.erp.products.repository.SaleLineRepository;
import com.erp.products.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Règles métier : parent conteneur, variante vendable/stockable.
 */
@Service
@RequiredArgsConstructor
public class ProductVariantPolicyService {

    public static final String MSG_SELECT_VARIANT = "Ce produit possède des variantes. Sélectionnez une variante.";
    public static final String MSG_PARENT_NOT_SELLABLE = "Ce produit possède des variantes et ne peut pas être vendu directement.";
    public static final String MSG_PARENT_NOT_STOCKABLE = "Ce produit possède des variantes. Sélectionnez une variante.";
    public static final String MSG_VARIANT_INACTIVE = "Cette variante est inactive.";
    public static final String MSG_VARIANT_NOT_SELLABLE = "Cette variante n'est pas vendable.";
    public static final String MSG_VARIANT_NOT_STOCKABLE = "Cette variante n'est pas stockable.";
    public static final String MSG_NO_VARIANTS = "Ce produit n'a pas de variantes.";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final SaleLineRepository saleLineRepository;
    private final StockItemRepository stockItemRepository;
    private final ProductEligibilityService productEligibilityService;

    @Transactional
    public void syncProductFlags(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + productId));
        long count = variantRepository.countByProductId(productId);
        boolean hasVariants = count > 0;
        product.setHasVariants(hasVariants);
        product.setIsSellable(!hasVariants);
        product.setIsStockable(!hasVariants);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public boolean hasVariants(Product product) {
        if (Boolean.TRUE.equals(product.getHasVariants())) {
            return true;
        }
        return variantRepository.countByProductId(product.getId()) > 0;
    }

    @Transactional(readOnly = true)
    public ProductVariant resolveForStock(Product product, Long variantId) {
        if (hasVariants(product)) {
            if (variantId == null) {
                throw new BusinessException(MSG_PARENT_NOT_STOCKABLE);
            }
            ProductVariant variant = loadVariant(product.getId(), variantId);
            assertStockable(variant);
            return variant;
        }
        if (variantId != null) {
            throw new BusinessException(MSG_NO_VARIANTS);
        }
        if (Boolean.FALSE.equals(product.getIsStockable())) {
            throw new BusinessException("Ce produit n'est pas stockable.");
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ProductVariant resolveForSale(Product product, Long variantId) {
        productEligibilityService.assertCommerciallyActive(product);
        if (hasVariants(product)) {
            if (variantId == null) {
                List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
                if (variants.size() == 1) {
                    ProductVariant only = variants.get(0);
                    assertSellable(only);
                    return only;
                }
                throw new BusinessException(MSG_SELECT_VARIANT);
            }
            ProductVariant variant = loadVariant(product.getId(), variantId);
            assertSellable(variant);
            return variant;
        }
        if (variantId != null) {
            throw new BusinessException(MSG_NO_VARIANTS);
        }
        if (Boolean.FALSE.equals(product.getIsSellable())) {
            throw new BusinessException(MSG_PARENT_NOT_SELLABLE);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public void assertSellable(ProductVariant variant) {
        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            throw new BusinessException(MSG_VARIANT_INACTIVE);
        }
        if (!Boolean.TRUE.equals(variant.getIsSellable())) {
            throw new BusinessException(MSG_VARIANT_NOT_SELLABLE);
        }
    }

    @Transactional(readOnly = true)
    public void assertStockable(ProductVariant variant) {
        if (!Boolean.TRUE.equals(variant.getIsStockable())) {
            throw new BusinessException(MSG_VARIANT_NOT_STOCKABLE);
        }
    }

    @Transactional(readOnly = true)
    public void assertDeletable(ProductVariant variant) {
        if (saleLineRepository.existsByVariantId(variant.getId())) {
            throw new BusinessException("Impossible : cette variante a déjà été vendue.");
        }
        BigDecimal stock = stockItemRepository.sumQuantityOnHandByVariantId(variant.getId());
        if (stock != null && stock.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Impossible : cette variante possède du stock.");
        }
    }

    @Transactional(readOnly = true)
    public String buildVariantName(ProductVariant variant) {
        if (variant.getName() != null && !variant.getName().isBlank()) {
            return variant.getName().trim();
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (variant.getCouleur() != null && !variant.getCouleur().isBlank()) {
            parts.add(variant.getCouleur().trim());
        }
        if (variant.getTaille() != null && !variant.getTaille().isBlank()) {
            parts.add(variant.getTaille().trim());
        }
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        if (variant.getAttributeValues() != null && !variant.getAttributeValues().isEmpty()) {
            return variant.getAttributeValues().stream()
                    .sorted(java.util.Comparator.comparing(v -> v.getAttribute().getCode()))
                    .map(v -> v.getAttributeValue().getValue())
                    .collect(java.util.stream.Collectors.joining(" "));
        }
        return variant.getSku();
    }

    @Transactional
    public void refreshVariantName(ProductVariant variant) {
        variant.setName(buildVariantName(variant));
    }

    private ProductVariant loadVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée: " + variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return variant;
    }

    public static int toDisplayStock(BigDecimal qty) {
        if (qty == null) {
            return 0;
        }
        return qty.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
