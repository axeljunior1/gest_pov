package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.PosSearchMatchType;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import com.erp.products.repository.SaleRepository;
import com.erp.products.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosCatalogService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final CategoryService categoryService;
    private final StockLedgerService ledger;
    private final SettingsService settingsService;
    private final SaleRepository saleRepository;
    private final ProductVariantAttributeService variantAttributeService;
    private final BarcodeLookupService barcodeLookupService;
    private final ProductEligibilityService productEligibilityService;

    @Transactional(readOnly = true)
    public PosCatalogResponse getCatalog(Long warehouseId, Long categoryId) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        applySellableProductFilters(criteria);
        if (categoryId != null) {
            criteria.setCategorieId(categoryId);
        }
        List<Product> products = productRepository.findAll(ProductSpecification.fromCriteria(criteria));

        List<PosProductResponse> mapped = products.stream()
                .map(p -> toPosProduct(p, warehouseId, null, null))
                .toList();

        List<PosProductResponse> promotions = mapped.stream()
                .filter(PosProductResponse::isPromotional)
                .limit(20)
                .toList();

        List<PosProductResponse> topSales = loadTopSales(warehouseId, 10);

        return PosCatalogResponse.builder()
                .categories(categoryService.getTree())
                .products(mapped)
                .favorites(List.of())
                .promotions(promotions)
                .recent(topSales)
                .topSales(topSales)
                .build();
    }

    @Transactional(readOnly = true)
    public PosSearchResultResponse search(String query, Long warehouseId, Long categoryId) {
        if (query == null || query.isBlank()) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.NONE)
                    .products(List.of())
                    .build();
        }
        String trimmed = query.trim();

        BarcodeScanConfig barcodeConfig = settingsService.getBarcodeScanConfig();
        if (barcodeConfig.isScanEnabled()
                && BarcodeLookupService.looksLikeBarcode(trimmed, barcodeConfig.getMinLength())) {
            return searchByBarcode(trimmed, warehouseId, categoryId);
        }

        Optional<ProductPackaging> byPackagingBarcode = packagingRepository.findActiveByCodeBarre(trimmed);
        if (byPackagingBarcode.isPresent()) {
            ProductPackaging packaging = byPackagingBarcode.get();
            Product product = packaging.getProduct();
            if (productEligibilityService.isCommerciallyActive(product)) {
                Long variantId = packaging.getVariant() != null ? packaging.getVariant().getId() : null;
                return PosSearchResultResponse.builder()
                        .matchType(PosSearchMatchType.EXACT_PACKAGING_BARCODE)
                        .products(List.of(toPosProduct(product, warehouseId, packaging.getId(), variantId)))
                        .build();
            }
        }

        Optional<ProductVariant> byVariantBarcode = variantRepository.findActiveSellableByCodeBarre(trimmed);
        if (byVariantBarcode.isPresent()) {
            ProductVariant variant = byVariantBarcode.get();
            Product product = variant.getProduct();
            if (productEligibilityService.isCommerciallyActive(product)) {
                return PosSearchResultResponse.builder()
                        .matchType(PosSearchMatchType.EXACT_BARCODE)
                        .products(List.of(toPosProduct(product, warehouseId, null, variant.getId())))
                        .build();
            }
        }

        Optional<Product> byProductBarcode = productRepository.findActiveSimpleByCodeBarre(trimmed);
        if (byProductBarcode.isPresent()) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.EXACT_BARCODE)
                    .products(List.of(toPosProduct(byProductBarcode.get(), warehouseId, null, null)))
                    .build();
        }

        Optional<Product> byBarcode = productRepository.findByVariantBarcode(trimmed);
        if (byBarcode.isPresent() && productEligibilityService.isCommerciallyActive(byBarcode.get())) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.EXACT_BARCODE)
                    .products(List.of(toPosProduct(byBarcode.get(), warehouseId, null, null)))
                    .build();
        }

        Optional<Product> bySku = productRepository.findBySku(trimmed);
        if (bySku.isPresent() && productEligibilityService.isCommerciallyActive(bySku.get())) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.EXACT_SKU)
                    .products(List.of(toPosProduct(bySku.get(), warehouseId, null, null)))
                    .build();
        }

        Optional<ProductVariant> byVariantSku = variantRepository.findBySku(trimmed);
        if (byVariantSku.isPresent()) {
            ProductVariant variant = byVariantSku.get();
            Product product = variant.getProduct();
            if (productEligibilityService.isCommerciallyActive(product)
                    && Boolean.TRUE.equals(variant.getIsActive())
                    && Boolean.TRUE.equals(variant.getIsSellable())) {
                return PosSearchResultResponse.builder()
                        .matchType(PosSearchMatchType.EXACT_SKU)
                        .products(List.of(toPosProduct(product, warehouseId, null, variant.getId())))
                        .build();
            }
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        applySellableProductFilters(criteria);
        criteria.setQuery(trimmed);
        if (categoryId != null) {
            criteria.setCategorieId(categoryId);
        }

        List<Product> results = new ArrayList<>(
                productRepository.findAll(ProductSpecification.fromCriteria(criteria)));
        if (results.isEmpty()) {
            String normalized = normalize(trimmed);
            ProductSearchCriteria broad = new ProductSearchCriteria();
            applySellableProductFilters(broad);
            if (categoryId != null) {
                broad.setCategorieId(categoryId);
            }
            productRepository.findAll(ProductSpecification.fromCriteria(broad)).stream()
                    .filter(p -> normalize(p.getNom()).contains(normalized)
                            || normalize(p.getSku()).contains(normalized))
                    .forEach(results::add);
        }

        String normalizedQuery = normalize(trimmed);
        Set<Long> seen = results.stream().map(Product::getId).collect(Collectors.toSet());
        productRepository.findAll(ProductSpecification.fromCriteria(buildActiveCriteria(categoryId))).stream()
                .filter(p -> !seen.contains(p.getId()))
                .filter(p -> variantRepository.findByProductId(p.getId()).stream()
                        .anyMatch(v -> matchesVariantSearch(v, normalizedQuery)))
                .forEach(p -> {
                    results.add(p);
                    seen.add(p.getId());
                });

        List<PosProductResponse> products = results.stream()
                .map(p -> toPosProduct(p, warehouseId, null, null))
                .limit(50)
                .toList();

        return PosSearchResultResponse.builder()
                .matchType(products.isEmpty() ? PosSearchMatchType.NONE : PosSearchMatchType.TEXT)
                .products(products)
                .build();
    }

    @Transactional(readOnly = true)
    public PosProductResponse getProduct(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve: " + productId));
        if (!productEligibilityService.isCommerciallyActive(product)) {
            throw new BusinessException(ProductEligibilityService.MSG_PRODUCT_INACTIVE);
        }
        return toPosProduct(product, warehouseId, null, null);
    }

    private void applySellableProductFilters(ProductSearchCriteria criteria) {
        criteria.setStatut(ProductStatus.ACTIF);
        criteria.setCycleVie(LifecycleStatus.ACTIF);
    }

    private ProductSearchCriteria buildActiveCriteria(Long categoryId) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        applySellableProductFilters(criteria);
        if (categoryId != null) {
            criteria.setCategorieId(categoryId);
        }
        return criteria;
    }

    private boolean matchesVariantSearch(ProductVariant variant, String normalizedQuery) {
        if (normalize(variant.getSku()).contains(normalizedQuery)) {
            return true;
        }
        if (variant.getName() != null && normalize(variant.getName()).contains(normalizedQuery)) {
            return true;
        }
        String label = variantAttributeService.buildVariantLabel(variant);
        if (label != null && normalize(label).contains(normalizedQuery)) {
            return true;
        }
        if (variant.getAttributeValues() != null) {
            return variant.getAttributeValues().stream()
                    .anyMatch(av -> normalize(av.getAttributeValue().getValue()).contains(normalizedQuery)
                            || normalize(av.getAttribute().getName()).contains(normalizedQuery));
        }
        return normalize(variant.getCouleur()).contains(normalizedQuery)
                || normalize(variant.getTaille()).contains(normalizedQuery);
    }

    private List<PosProductResponse> loadTopSales(Long warehouseId, int limit) {
        List<Object[]> rows = saleRepository.findTopSoldProductIds(PageRequest.of(0, limit));
        List<PosProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            productRepository.findById(productId)
                    .filter(productEligibilityService::isCommerciallyActive)
                    .ifPresent(p -> result.add(toPosProduct(p, warehouseId, null, null)));
        }
        return result;
    }

    private PosProductResponse toPosProduct(Product product, Long warehouseId,
                                            Long matchedPackagingId, Long matchedVariantId) {
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId()).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()) && Boolean.TRUE.equals(v.getIsSellable()))
                .toList();
        List<PosVariantResponse> posVariants = variants.stream()
                .map(v -> toPosVariant(v, product, warehouseId))
                .toList();

        BigDecimal available = warehouseId != null
                ? ledger.getAvailable(product.getId(), null, warehouseId)
                : ledger.getAvailable(product.getId(), null, null);

        BigDecimal threshold = settingsService.getStockConfig().getLowStockThresholdDefault();
        boolean outOfStock = available.compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock && threshold != null && available.compareTo(threshold) <= 0;

        BigDecimal unitPrice = PosSaleService.resolveUnitPrice(product);
        boolean promotional = product.getPrixPromotionnel() != null
                && unitPrice.equals(product.getPrixPromotionnel());

        List<String> barcodes = new ArrayList<>();
        if (product.getCodeBarre() != null && !product.getCodeBarre().isBlank()) {
            barcodes.add(product.getCodeBarre());
        }
        barcodes.addAll(variants.stream()
                .map(ProductVariant::getCodeBarre)
                .filter(Objects::nonNull)
                .filter(b -> !b.isBlank())
                .toList());
        packagingRepository.findByProductIdAndActifTrueOrderByNomAsc(product.getId()).stream()
                .map(ProductPackaging::getCodeBarre)
                .filter(Objects::nonNull)
                .filter(b -> !b.isBlank())
                .forEach(barcodes::add);

        String imageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0).getFilePath() : null;

        List<PosPackagingResponse> packagings = packagingRepository
                .findByProductIdAndActifTrueOrderByNomAsc(product.getId()).stream()
                .map(this::toPosPackaging)
                .toList();

        boolean hasVariants = Boolean.TRUE.equals(product.getHasVariants()) || !variants.isEmpty();
        boolean stockable = !Boolean.FALSE.equals(product.getIsStockable());
        boolean requiresVariantSelection = posVariants.size() > 1;
        boolean sellable = productEligibilityService.isCommerciallyActive(product)
                && !Boolean.FALSE.equals(product.getIsSellable())
                && (!hasVariants || !posVariants.isEmpty());

        return PosProductResponse.builder()
                .id(product.getId())
                .nom(product.getNom())
                .sku(product.getSku())
                .categoryId(product.getCategorie() != null ? product.getCategorie().getId() : null)
                .categoryNom(product.getCategorie() != null ? product.getCategorie().getNom() : null)
                .unitPrice(unitPrice)
                .promotional(promotional)
                .stockAvailable(available)
                .outOfStock(outOfStock)
                .lowStock(lowStock)
                .sellable(sellable)
                .hasVariants(hasVariants)
                .stockable(stockable)
                .requiresVariantSelection(requiresVariantSelection)
                .variants(posVariants)
                .imageUrl(imageUrl)
                .barcodes(barcodes)
                .packagings(packagings)
                .matchedPackagingId(matchedPackagingId)
                .matchedVariantId(matchedVariantId)
                .build();
    }

    private PosVariantResponse toPosVariant(ProductVariant variant, Product product, Long warehouseId) {
        BigDecimal stock = warehouseId != null
                ? ledger.getAvailable(product.getId(), variant.getId(), warehouseId)
                : ledger.getAvailable(product.getId(), variant.getId(), null);
        BigDecimal unitPrice = variant.getPrix() != null ? variant.getPrix() : PosSaleService.resolveUnitPrice(product);

        BigDecimal threshold = settingsService.getStockConfig().getLowStockThresholdDefault();
        boolean outOfStock = stock.compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock && threshold != null && stock.compareTo(threshold) <= 0;

        List<VariantAttributeSelectionResponse> attributes = variant.getAttributeValues() == null
                ? List.of()
                : variant.getAttributeValues().stream()
                .sorted(Comparator.comparing(v -> v.getAttribute().getCode()))
                .map(v -> VariantAttributeSelectionResponse.builder()
                        .attributeId(v.getAttribute().getId())
                        .attributeCode(v.getAttribute().getCode())
                        .attributeName(v.getAttribute().getName())
                        .valueId(v.getAttributeValue().getId())
                        .value(v.getAttributeValue().getValue())
                        .valueCode(v.getAttributeValue().getCode())
                        .build())
                .toList();

        List<PosPackagingResponse> packagings = packagingRepository
                .findByProductIdAndActifTrueOrderByNomAsc(product.getId()).stream()
                .filter(p -> p.getVariant() == null || p.getVariant().getId().equals(variant.getId()))
                .map(this::toPosPackaging)
                .toList();

        return PosVariantResponse.builder()
                .id(variant.getId())
                .label(variantAttributeService.buildVariantLabel(variant))
                .sku(variant.getSku())
                .unitPrice(unitPrice)
                .stockAvailable(stock)
                .outOfStock(outOfStock)
                .lowStock(lowStock)
                .codeBarre(variant.getCodeBarre())
                .attributes(attributes)
                .packagings(packagings)
                .build();
    }

    private PosPackagingResponse toPosPackaging(ProductPackaging packaging) {
        return PosPackagingResponse.builder()
                .id(packaging.getId())
                .nom(packaging.getNom())
                .quantiteBase(packaging.getQuantiteBase())
                .salePrice(packaging.getPrixVente())
                .codeBarre(packaging.getCodeBarre())
                .defaultSale(Boolean.TRUE.equals(packaging.getDefaultVente()))
                .active(Boolean.TRUE.equals(packaging.getActif()))
                .build();
    }

    private PosSearchResultResponse searchByBarcode(String trimmed, Long warehouseId, Long categoryId) {
        if (categoryId != null) {
            // Le scan ignore le filtre catégorie pour retrouver l'article partout.
        }
        List<BarcodeLookupResult> matches;
        try {
            matches = barcodeLookupService.lookupBarcode(trimmed);
        } catch (BusinessException e) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.BARCODE_NOT_FOUND)
                    .message(e.getMessage())
                    .products(List.of())
                    .build();
        }

        if (matches.isEmpty()) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.BARCODE_NOT_FOUND)
                    .message("Aucun produit trouvé pour ce code-barres")
                    .products(List.of())
                    .build();
        }
        if (matches.size() > 1) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.BARCODE_AMBIGUOUS)
                    .ambiguous(true)
                    .message("Plusieurs articles correspondent à ce code-barres")
                    .barcodeMatches(matches)
                    .products(matches.stream()
                            .map(m -> productRepository.findById(m.getProductId()).orElse(null))
                            .filter(Objects::nonNull)
                            .map(p -> toPosProduct(p, warehouseId, null, null))
                            .toList())
                    .build();
        }

        BarcodeLookupResult lookup = matches.get(0);
        Product product = productRepository.findById(lookup.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + lookup.getProductId()));

        PosSearchMatchType matchType = lookup.getType() == com.erp.products.domain.enums.BarcodeLookupType.PACKAGING
                ? PosSearchMatchType.EXACT_PACKAGING_BARCODE
                : PosSearchMatchType.EXACT_BARCODE;

        return PosSearchResultResponse.builder()
                .matchType(matchType)
                .products(List.of(toPosProduct(
                        product, warehouseId, lookup.getPackagingId(), lookup.getVariantId())))
                .build();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
