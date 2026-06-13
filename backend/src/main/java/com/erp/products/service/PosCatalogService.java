package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.PosSearchMatchType;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.*;
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

    @Transactional(readOnly = true)
    public PosCatalogResponse getCatalog(Long warehouseId, Long categoryId) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setStatut(ProductStatus.ACTIF);
        if (categoryId != null) {
            criteria.setCategorieId(categoryId);
        }
        List<Product> products = productRepository.findAll(ProductSpecification.fromCriteria(criteria));

        List<PosProductResponse> mapped = products.stream()
                .map(p -> toPosProduct(p, warehouseId, null))
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

        Optional<ProductPackaging> byPackagingBarcode = packagingRepository.findActiveByCodeBarre(trimmed);
        if (byPackagingBarcode.isPresent()) {
            ProductPackaging packaging = byPackagingBarcode.get();
            Product product = packaging.getProduct();
            if (product.getStatut() == ProductStatus.ACTIF) {
                return PosSearchResultResponse.builder()
                        .matchType(PosSearchMatchType.EXACT_PACKAGING_BARCODE)
                        .products(List.of(toPosProduct(product, warehouseId, packaging.getId())))
                        .build();
            }
        }

        Optional<Product> byBarcode = productRepository.findByVariantBarcode(trimmed);
        if (byBarcode.isPresent()) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.EXACT_BARCODE)
                    .products(List.of(toPosProduct(byBarcode.get(), warehouseId, null)))
                    .build();
        }

        Optional<Product> bySku = productRepository.findBySku(trimmed);
        if (bySku.isPresent()) {
            return PosSearchResultResponse.builder()
                    .matchType(PosSearchMatchType.EXACT_SKU)
                    .products(List.of(toPosProduct(bySku.get(), warehouseId, null)))
                    .build();
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setStatut(ProductStatus.ACTIF);
        criteria.setQuery(trimmed);
        if (categoryId != null) {
            criteria.setCategorieId(categoryId);
        }

        List<Product> results = productRepository.findAll(ProductSpecification.fromCriteria(criteria));
        if (results.isEmpty()) {
            String normalized = normalize(trimmed);
            ProductSearchCriteria broad = new ProductSearchCriteria();
            broad.setStatut(ProductStatus.ACTIF);
            if (categoryId != null) {
                broad.setCategorieId(categoryId);
            }
            results = productRepository.findAll(ProductSpecification.fromCriteria(broad)).stream()
                    .filter(p -> normalize(p.getNom()).contains(normalized)
                            || normalize(p.getSku()).contains(normalized))
                    .toList();
        }

        List<PosProductResponse> products = results.stream()
                .map(p -> toPosProduct(p, warehouseId, null))
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
        return toPosProduct(product, warehouseId, null);
    }

    private List<PosProductResponse> loadTopSales(Long warehouseId, int limit) {
        List<Object[]> rows = saleRepository.findTopSoldProductIds(PageRequest.of(0, limit));
        List<PosProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            productRepository.findById(productId)
                    .ifPresent(p -> result.add(toPosProduct(p, warehouseId, null)));
        }
        return result;
    }

    private PosProductResponse toPosProduct(Product product, Long warehouseId, Long matchedPackagingId) {
        BigDecimal available = warehouseId != null
                ? ledger.getAvailable(product.getId(), null, warehouseId)
                : ledger.getAvailable(product.getId(), null, null);

        BigDecimal threshold = settingsService.getStockConfig().getLowStockThresholdDefault();
        boolean outOfStock = available.compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock && threshold != null && available.compareTo(threshold) <= 0;

        BigDecimal unitPrice = PosSaleService.resolveUnitPrice(product);
        boolean promotional = product.getPrixPromotionnel() != null
                && unitPrice.equals(product.getPrixPromotionnel());

        List<String> barcodes = new ArrayList<>(variantRepository.findByProductId(product.getId()).stream()
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
                .imageUrl(imageUrl)
                .barcodes(barcodes)
                .packagings(packagings)
                .matchedPackagingId(matchedPackagingId)
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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
