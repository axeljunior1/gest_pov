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
import com.erp.products.security.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackagingService {

    private final ProductPackagingRepository packagingRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;
    private final PermissionEvaluator permissionChecker;

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

        boolean defaultVente = Boolean.TRUE.equals(request.getDefaultVente());
        boolean defaultAchat = Boolean.TRUE.equals(request.getDefaultAchat())
                || Boolean.TRUE.equals(request.getPrincipal());
        if (defaultVente) {
            clearDefaultVente(product);
        }
        if (defaultAchat) {
            clearDefaultAchat(product);
        }

        BigDecimal prixVente = resolvePrixVente(product, request.getQuantiteBase(), request.getPrixVente());

        ProductPackaging packaging = ProductPackaging.builder()
                .product(product)
                .nom(request.getNom().trim())
                .symbole(request.getSymbole())
                .quantiteBase(request.getQuantiteBase())
                .codeBarre(trimToNull(request.getCodeBarre()))
                .prixVente(prixVente)
                .prixAchat(request.getPrixAchat())
                .defaultVente(defaultVente)
                .defaultAchat(defaultAchat)
                .principal(defaultAchat)
                .actif(request.getActif() == null || request.getActif())
                .build();

        ProductPackaging saved = packagingRepository.save(packaging);
        product.getConditionnements().add(saved);

        auditService.log("Product", productId, AuditAction.MODIFICATION,
                "Conditionnement ajouté: " + saved.getNom() + " (1 = " + saved.getQuantiteBase() + " "
                        + product.getUnit().getSymbole() + ", prix vente " + saved.getPrixVente() + ")");

        return mapper.toPackagingResponse(saved, product);
    }

    @Transactional
    public ProductPackagingResponse update(Long productId, Long packagingId, ProductPackagingRequest request) {
        Product product = findProduct(productId);
        ProductPackaging packaging = findPackaging(productId, packagingId);

        BigDecimal newPrixVente = request.getPrixVente() != null
                ? request.getPrixVente()
                : resolvePrixVente(product, request.getQuantiteBase(), null);
        if (newPrixVente.compareTo(packaging.getPrixVente()) != 0 && !canUpdatePackagingPrice()) {
            throw new BusinessException("Permission requise pour modifier le prix de vente du conditionnement");
        }

        boolean defaultVente = Boolean.TRUE.equals(request.getDefaultVente());
        boolean defaultAchat = Boolean.TRUE.equals(request.getDefaultAchat())
                || Boolean.TRUE.equals(request.getPrincipal());
        if (defaultVente) {
            clearDefaultVente(product);
        }
        if (defaultAchat) {
            clearDefaultAchat(product);
        }

        packaging.setNom(request.getNom().trim());
        packaging.setSymbole(request.getSymbole());
        packaging.setQuantiteBase(request.getQuantiteBase());
        packaging.setCodeBarre(trimToNull(request.getCodeBarre()));
        packaging.setPrixVente(newPrixVente);
        packaging.setPrixAchat(request.getPrixAchat());
        packaging.setDefaultVente(defaultVente);
        packaging.setDefaultAchat(defaultAchat);
        packaging.setPrincipal(defaultAchat);
        if (request.getActif() != null) {
            packaging.setActif(request.getActif());
        }

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

    static BigDecimal resolvePrixVente(Product product, BigDecimal quantiteBase, BigDecimal requested) {
        if (requested != null) {
            return requested.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal unitPrice = PosSaleService.resolveUnitPrice(product);
        return unitPrice.multiply(quantiteBase).setScale(4, RoundingMode.HALF_UP);
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

    private void clearDefaultVente(Product product) {
        product.getConditionnements().forEach(p -> p.setDefaultVente(false));
    }

    private void clearDefaultAchat(Product product) {
        product.getConditionnements().forEach(p -> {
            p.setDefaultAchat(false);
            p.setPrincipal(false);
        });
    }

    private boolean canUpdatePackagingPrice() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return permissionChecker.has(auth, "product_packaging.update_price");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
