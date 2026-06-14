package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.domain.enums.PackagingUsageContext;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import com.erp.products.security.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
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
    private final ProductVariantRepository variantRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;
    private final PermissionEvaluator permissionChecker;
    private final ProductVariantPolicyService variantPolicyService;
    private final BarcodeRegistryService barcodeRegistryService;

    @Transactional(readOnly = true)
    public List<ProductPackagingResponse> listByProduct(Long productId) {
        return listByProduct(productId, null);
    }

    @Transactional(readOnly = true)
    public List<ProductPackagingResponse> listByProduct(Long productId, PackagingUsageContext context) {
        Product product = findProduct(productId);
        return packagingRepository.findByProductIdOrderByNomAsc(productId).stream()
                .filter(p -> matchesContext(p, context))
                .map(p -> mapper.toPackagingResponse(p, product))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductPackagingResponse create(Long productId, ProductPackagingRequest request) {
        Product product = findProduct(productId);
        requireBaseUnit(product);
        ProductVariant variant = resolvePackagingVariant(product, request.getVariantId());

        boolean defaultVente = Boolean.TRUE.equals(request.getDefaultVente());
        boolean defaultAchat = Boolean.TRUE.equals(request.getDefaultAchat())
                || Boolean.TRUE.equals(request.getPrincipal());
        boolean usableForSale = resolveUsableForSale(request, defaultVente);
        boolean usableForPurchase = resolveUsableForPurchase(request, defaultAchat);
        assertUsageConsistency(defaultVente, defaultAchat, usableForSale, usableForPurchase);

        if (defaultVente) {
            clearDefaultVente(product);
        }
        if (defaultAchat) {
            clearDefaultAchat(product);
        }

        BigDecimal prixVente = resolvePrixVente(product, request.getQuantiteBase(), request.getPrixVente());

        ProductPackaging packaging = ProductPackaging.builder()
                .product(product)
                .variant(variant)
                .nom(request.getNom().trim())
                .symbole(request.getSymbole())
                .quantiteBase(request.getQuantiteBase())
                .codeBarre(trimToNull(request.getCodeBarre()))
                .prixVente(prixVente)
                .prixAchat(request.getPrixAchat())
                .defaultVente(defaultVente)
                .defaultAchat(defaultAchat)
                .usableForSale(usableForSale)
                .usableForPurchase(usableForPurchase)
                .principal(defaultAchat)
                .actif(request.getActif() == null || request.getActif())
                .build();

        assertPackagingBarcode(packaging);

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
        ProductVariant variant = resolvePackagingVariant(product, request.getVariantId());

        BigDecimal newPrixVente = request.getPrixVente() != null
                ? request.getPrixVente()
                : resolvePrixVente(product, request.getQuantiteBase(), null);
        if (newPrixVente.compareTo(packaging.getPrixVente()) != 0 && !canUpdatePackagingPrice()) {
            throw new BusinessException("Permission requise pour modifier le prix de vente du conditionnement");
        }

        boolean defaultVente = Boolean.TRUE.equals(request.getDefaultVente());
        boolean defaultAchat = Boolean.TRUE.equals(request.getDefaultAchat())
                || Boolean.TRUE.equals(request.getPrincipal());
        boolean usableForSale = resolveUsableForSale(request, defaultVente);
        boolean usableForPurchase = resolveUsableForPurchase(request, defaultAchat);
        assertUsageConsistency(defaultVente, defaultAchat, usableForSale, usableForPurchase);

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
        packaging.setVariant(variant);
        packaging.setDefaultVente(defaultVente);
        packaging.setDefaultAchat(defaultAchat);
        packaging.setUsableForSale(usableForSale);
        packaging.setUsableForPurchase(usableForPurchase);
        packaging.setPrincipal(defaultAchat);
        if (request.getActif() != null) {
            packaging.setActif(request.getActif());
        }

        assertPackagingBarcode(packaging);

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

    private ProductVariant resolvePackagingVariant(Product product, Long variantId) {
        if (!variantPolicyService.hasVariants(product)) {
            if (variantId != null) {
                throw new BusinessException(ProductVariantPolicyService.MSG_NO_VARIANTS);
            }
            return null;
        }
        if (variantId != null) {
            return loadVariant(product, variantId);
        }
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
        if (variants.size() == 1) {
            return variants.get(0);
        }
        throw new BusinessException(
                "Ce produit possède des variantes. Sélectionnez la variante pour ce conditionnement.");
    }

    private ProductVariant loadVariant(Product product, Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée: " + variantId));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return variant;
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
        product.getConditionnements().forEach(p -> {
            if (Boolean.TRUE.equals(p.getUsableForSale())) {
                p.setDefaultVente(false);
            }
        });
    }

    private void clearDefaultAchat(Product product) {
        product.getConditionnements().forEach(p -> {
            if (Boolean.TRUE.equals(p.getUsableForPurchase())) {
                p.setDefaultAchat(false);
                p.setPrincipal(false);
            }
        });
    }

    static boolean resolveUsableForSale(ProductPackagingRequest request, boolean defaultVente) {
        if (request.getUsableForSale() != null) {
            return Boolean.TRUE.equals(request.getUsableForSale());
        }
        return true;
    }

    static boolean resolveUsableForPurchase(ProductPackagingRequest request, boolean defaultAchat) {
        if (request.getUsableForPurchase() != null) {
            return Boolean.TRUE.equals(request.getUsableForPurchase());
        }
        return true;
    }

    static void assertUsageConsistency(
            boolean defaultVente,
            boolean defaultAchat,
            boolean usableForSale,
            boolean usableForPurchase) {
        if (defaultVente && !usableForSale) {
            throw new BusinessException("Un conditionnement vente par defaut doit etre utilisable a la vente");
        }
        if (defaultAchat && !usableForPurchase) {
            throw new BusinessException("Un conditionnement achat par defaut doit etre utilisable a l'achat");
        }
        if (!usableForSale && !usableForPurchase) {
            throw new BusinessException("Le conditionnement doit etre utilisable en vente et/ou en achat");
        }
    }

    static boolean matchesContext(ProductPackaging packaging, PackagingUsageContext context) {
        if (context == null) {
            return true;
        }
        return switch (context) {
            case SALE -> Boolean.TRUE.equals(packaging.getUsableForSale());
            case PURCHASE -> Boolean.TRUE.equals(packaging.getUsableForPurchase());
        };
    }

    public static void assertUsableForSale(ProductPackaging packaging) {
        if (packaging != null && !Boolean.TRUE.equals(packaging.getUsableForSale())) {
            throw new BusinessException("Ce conditionnement n'est pas utilisable a la vente");
        }
    }

    public static void assertUsableForPurchase(ProductPackaging packaging) {
        if (packaging != null && !Boolean.TRUE.equals(packaging.getUsableForPurchase())) {
            throw new BusinessException("Ce conditionnement n'est pas utilisable a l'achat");
        }
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

    private void assertPackagingBarcode(ProductPackaging packaging) {
        if (packaging.getCodeBarre() == null) {
            return;
        }
        barcodeRegistryService.assertAvailable(
                packaging.getCodeBarre(), null, null, packaging.getId());
    }
}
