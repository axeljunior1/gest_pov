package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.ResourceNotFoundException;
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
                .map(p -> toPosProduct(p, warehouseId))
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
    public List<PosProductResponse> search(String query, Long warehouseId, Long categoryId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();

        Optional<Product> byBarcode = productRepository.findByVariantBarcode(trimmed);
        if (byBarcode.isPresent()) {
            return List.of(toPosProduct(byBarcode.get(), warehouseId));
        }

        Optional<Product> bySku = productRepository.findBySku(trimmed);
        if (bySku.isPresent()) {
            return List.of(toPosProduct(bySku.get(), warehouseId));
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
            results = productRepository.findAll(ProductSpecification.fromCriteria(criteria)).stream()
                    .filter(p -> normalize(p.getNom()).contains(normalized)
                            || normalize(p.getSku()).contains(normalized))
                    .toList();
        }

        return results.stream()
                .map(p -> toPosProduct(p, warehouseId))
                .limit(50)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosProductResponse getProduct(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve: " + productId));
        return toPosProduct(product, warehouseId);
    }

    private List<PosProductResponse> loadTopSales(Long warehouseId, int limit) {
        List<Object[]> rows = saleRepository.findTopSoldProductIds(PageRequest.of(0, limit));
        List<PosProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            productRepository.findById(productId)
                    .ifPresent(p -> result.add(toPosProduct(p, warehouseId)));
        }
        return result;
    }

    private PosProductResponse toPosProduct(Product product, Long warehouseId) {
        BigDecimal available = warehouseId != null
                ? ledger.getAvailable(product.getId(), null, warehouseId)
                : ledger.getAvailable(product.getId(), null, null);

        BigDecimal threshold = settingsService.getStockConfig().getLowStockThresholdDefault();
        boolean outOfStock = available.compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock && threshold != null && available.compareTo(threshold) <= 0;

        BigDecimal unitPrice = PosSaleService.resolveUnitPrice(product);
        boolean promotional = product.getPrixPromotionnel() != null
                && unitPrice.equals(product.getPrixPromotionnel());

        List<String> barcodes = variantRepository.findByProductId(product.getId()).stream()
                .map(ProductVariant::getCodeBarre)
                .filter(Objects::nonNull)
                .filter(b -> !b.isBlank())
                .collect(Collectors.toList());

        String imageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0).getFilePath() : null;

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
