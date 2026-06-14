package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.BarcodeLookupType;
import com.erp.products.dto.BarcodeLookupResult;
import com.erp.products.dto.BarcodeScanConfig;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLookupService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final ProductVariantAttributeService variantAttributeService;
    private final ProductVariantPolicyService variantPolicyService;
    private final SettingsService settingsService;
    private final ProductEligibilityService productEligibilityService;

    @Transactional(readOnly = true)
    public List<BarcodeLookupResult> lookupBarcode(String code) {
        String normalized = BarcodeRegistryService.normalize(code);
        if (normalized == null) {
            return List.of();
        }

        BarcodeScanConfig config = settingsService.getBarcodeScanConfig();
        List<String> priority = parsePriority(config.getSearchPriority());

        List<BarcodeLookupResult> matches = new ArrayList<>();
        for (String level : priority) {
            switch (level) {
                case "packaging" -> packagingRepository.findActiveByCodeBarre(normalized)
                        .map(this::toPackagingResult)
                        .ifPresent(matches::add);
                case "variant" -> variantRepository.findActiveSellableByCodeBarre(normalized)
                        .map(this::toVariantResult)
                        .ifPresent(matches::add);
                case "product" -> productRepository.findActiveSimpleByCodeBarre(normalized)
                        .map(this::toProductResult)
                        .ifPresent(matches::add);
                default -> { }
            }
        }
        return matches;
    }

    @Transactional(readOnly = true)
    public Optional<BarcodeLookupResult> lookupSingle(String code) {
        List<BarcodeLookupResult> matches = lookupBarcode(code);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            log.warn("Code-barres ambigu {} : {} correspondances", BarcodeRegistryService.normalize(code), matches.size());
            throw new BusinessException(
                    "Plusieurs articles correspondent à ce code-barres. Contactez un administrateur.");
        }
        return Optional.of(matches.get(0));
    }

    public static boolean looksLikeBarcode(String query, int minLength) {
        if (query == null) {
            return false;
        }
        String trimmed = query.trim();
        return trimmed.matches("\\d+") && trimmed.length() >= minLength;
    }

    private BarcodeLookupResult toPackagingResult(ProductPackaging packaging) {
        Product product = packaging.getProduct();
        productEligibilityService.assertCommerciallyActive(product);
        ProductVariant variant = packaging.getVariant();
        if (variant != null) {
            assertVariantSellable(variant);
        } else if (variantPolicyService.hasVariants(product)) {
            throw new BusinessException(
                    "Ce produit possède des variantes. Utilisez le code-barres d'une variante ou d'un conditionnement.");
        } else if (Boolean.FALSE.equals(product.getIsSellable())) {
            throw new BusinessException("Ce produit n'est pas vendable");
        }

        String display = product.getNom();
        if (variant != null) {
            display += " — " + variantAttributeService.buildVariantLabel(variant);
        }
        display += " — " + packaging.getNom();

        return BarcodeLookupResult.builder()
                .type(BarcodeLookupType.PACKAGING)
                .productId(product.getId())
                .variantId(variant != null ? variant.getId() : null)
                .packagingId(packaging.getId())
                .displayName(display)
                .salePrice(packaging.getPrixVente())
                .quantityInBaseUnit(packaging.getQuantiteBase())
                .barcode(packaging.getCodeBarre())
                .build();
    }

    private BarcodeLookupResult toVariantResult(ProductVariant variant) {
        Product product = variant.getProduct();
        productEligibilityService.assertCommerciallyActive(product);
        assertVariantSellable(variant);

        ProductPackaging defaultPackaging = findDefaultPackaging(product, variant.getId());
        BigDecimal price = variant.getPrix() != null ? variant.getPrix() : PosSaleService.resolveUnitPrice(product);
        Long packagingId = null;
        if (defaultPackaging != null) {
            price = defaultPackaging.getPrixVente();
            packagingId = defaultPackaging.getId();
        }

        return BarcodeLookupResult.builder()
                .type(BarcodeLookupType.VARIANT)
                .productId(product.getId())
                .variantId(variant.getId())
                .packagingId(packagingId)
                .displayName(product.getNom() + " — " + variantAttributeService.buildVariantLabel(variant))
                .salePrice(price)
                .quantityInBaseUnit(BigDecimal.ONE)
                .barcode(variant.getCodeBarre())
                .build();
    }

    private BarcodeLookupResult toProductResult(Product product) {
        productEligibilityService.assertCommerciallyActive(product);
        if (variantPolicyService.hasVariants(product)) {
            throw new BusinessException(
                    "Ce produit possède des variantes. Scannez le code-barres d'une variante ou d'un conditionnement.");
        }
        if (Boolean.FALSE.equals(product.getIsSellable())) {
            throw new BusinessException("Ce produit n'est pas vendable");
        }

        ProductPackaging defaultPackaging = findDefaultPackaging(product, null);
        BigDecimal price = PosSaleService.resolveUnitPrice(product);
        Long packagingId = null;
        if (defaultPackaging != null) {
            price = defaultPackaging.getPrixVente();
            packagingId = defaultPackaging.getId();
        }

        return BarcodeLookupResult.builder()
                .type(BarcodeLookupType.PRODUCT)
                .productId(product.getId())
                .variantId(null)
                .packagingId(packagingId)
                .displayName(product.getNom())
                .salePrice(price)
                .quantityInBaseUnit(BigDecimal.ONE)
                .barcode(product.getCodeBarre())
                .build();
    }

    private ProductPackaging findDefaultPackaging(Product product, Long variantId) {
        return packagingRepository.findByProductIdAndActifTrueOrderByNomAsc(product.getId()).stream()
                .filter(p -> p.getVariant() == null
                        || (variantId != null && p.getVariant().getId().equals(variantId)))
                .filter(p -> Boolean.TRUE.equals(p.getDefaultVente()))
                .findFirst()
                .orElse(null);
    }

    private void assertVariantSellable(ProductVariant variant) {
        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            throw new BusinessException("Cette variante est inactive");
        }
        if (!Boolean.TRUE.equals(variant.getIsSellable())) {
            throw new BusinessException("Cette variante n'est pas vendable");
        }
    }

    private List<String> parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return List.of("packaging", "variant", "product");
        }
        return java.util.Arrays.stream(priority.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
