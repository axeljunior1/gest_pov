package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackagingService {

    private final ProductPackagingRepository packagingRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProductPackagingResponse> listByProduct(Long productId) {
        Product product = findProduct(productId);
        return packagingRepository.findByProductIdOrderByNomAsc(productId).stream()
                .map(p -> mapper.toPackagingResponse(p, product))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductPackagingResponse create(Long productId, ProductPackagingRequest request) {
        Product product = findProduct(productId);
        requireBaseUnit(product);

        if (Boolean.TRUE.equals(request.getPrincipal())) {
            clearPrincipal(product);
        }

        ProductPackaging packaging = ProductPackaging.builder()
                .product(product)
                .nom(request.getNom().trim())
                .symbole(request.getSymbole())
                .quantiteBase(request.getQuantiteBase())
                .codeBarre(request.getCodeBarre())
                .principal(Boolean.TRUE.equals(request.getPrincipal()))
                .build();

        ProductPackaging saved = packagingRepository.save(packaging);
        product.getConditionnements().add(saved);

        auditService.log("Product", productId, AuditAction.MODIFICATION,
                "Conditionnement ajouté: " + saved.getNom() + " (1 = " + saved.getQuantiteBase() + " "
                        + product.getUnit().getSymbole() + ")");

        return mapper.toPackagingResponse(saved, product);
    }

    @Transactional
    public ProductPackagingResponse update(Long productId, Long packagingId, ProductPackagingRequest request) {
        Product product = findProduct(productId);
        ProductPackaging packaging = findPackaging(productId, packagingId);

        if (Boolean.TRUE.equals(request.getPrincipal())) {
            clearPrincipal(product);
        }

        packaging.setNom(request.getNom().trim());
        packaging.setSymbole(request.getSymbole());
        packaging.setQuantiteBase(request.getQuantiteBase());
        packaging.setCodeBarre(request.getCodeBarre());
        packaging.setPrincipal(Boolean.TRUE.equals(request.getPrincipal()));

        return mapper.toPackagingResponse(packagingRepository.save(packaging), product);
    }

    @Transactional
    public void delete(Long productId, Long packagingId) {
        ProductPackaging packaging = findPackaging(productId, packagingId);
        packagingRepository.delete(packaging);
        auditService.log("Product", productId, AuditAction.MODIFICATION,
                "Conditionnement supprimé: " + packaging.getNom());
    }

    /**
     * Convertit une quantité en conditionnement vers l'unité de base du produit.
     * Ex: 3 cartons × 12 bouteilles/carton = 36 bouteilles
     */
    @Transactional(readOnly = true)
    public PackagingToBaseResponse convertToBase(Long productId, PackagingToBaseRequest request) {
        Product product = findProduct(productId);
        requireBaseUnit(product);
        ProductPackaging packaging = findPackaging(productId, request.getPackagingId());

        BigDecimal baseQty = request.getQuantity()
                .multiply(packaging.getQuantiteBase())
                .setScale(4, RoundingMode.HALF_UP);

        String explanation = String.format("%s %s × %s %s/%s = %s %s",
                request.getQuantity().stripTrailingZeros().toPlainString(),
                packaging.getNom(),
                packaging.getQuantiteBase().stripTrailingZeros().toPlainString(),
                product.getUnit().getSymbole(),
                packaging.getNom(),
                baseQty.stripTrailingZeros().toPlainString(),
                product.getUnit().getSymbole());

        return PackagingToBaseResponse.builder()
                .productId(productId)
                .packagingId(packaging.getId())
                .packagingNom(packaging.getNom())
                .quantityPackaging(request.getQuantity())
                .quantiteBase(baseQty)
                .baseUnitSymbole(product.getUnit().getSymbole())
                .baseUnitNom(product.getUnit().getNom())
                .explanation(explanation)
                .build();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + id));
    }

    private ProductPackaging findPackaging(Long productId, Long packagingId) {
        return packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouvé"));
    }

    private void requireBaseUnit(Product product) {
        if (product.getUnit() == null) {
            throw new BusinessException(
                    "Définissez d'abord l'unité de base du produit (ex: bouteille, pièce, kg)");
        }
    }

    private void clearPrincipal(Product product) {
        product.getConditionnements().forEach(p -> p.setPrincipal(false));
    }
}
