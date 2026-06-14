package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.dto.ProductVariantGenerateRequest;
import com.erp.products.dto.ProductVariantRequest;
import com.erp.products.dto.VariantAttributeSelectionRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantAttributeService {

    private final VariantAttributeRepository attributeRepository;
    private final VariantAttributeValueRepository valueRepository;
    private final ProductVariantAttributeValueRepository variantAttributeValueRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final BarcodeRegistryService barcodeRegistryService;

    @Transactional
    public void syncProductSellable(Long productId) {
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
    public List<VariantAttributeSelectionRequest> resolveSelections(ProductVariantRequest request) {
        if (request.getAttributeSelections() != null && !request.getAttributeSelections().isEmpty()) {
            return request.getAttributeSelections();
        }
        List<VariantAttributeSelectionRequest> legacy = new ArrayList<>();
        if (request.getCouleur() != null && !request.getCouleur().isBlank()) {
            legacy.add(selectionForLegacyValue("COULEUR", request.getCouleur().trim()));
        }
        if (request.getTaille() != null && !request.getTaille().isBlank()) {
            legacy.add(selectionForLegacyValue("TAILLE", request.getTaille().trim()));
        }
        return legacy;
    }

    @Transactional
    public void applySelections(ProductVariant variant, List<VariantAttributeSelectionRequest> selections) {
        variant.getAttributeValues().clear();
        if (selections == null || selections.isEmpty()) {
            return;
        }
        Set<Long> attributeIds = new HashSet<>();
        for (VariantAttributeSelectionRequest sel : selections) {
            if (sel.getAttributeId() == null || sel.getValueId() == null) {
                throw new BusinessException("Attribut et valeur requis pour chaque sélection");
            }
            if (!attributeIds.add(sel.getAttributeId())) {
                throw new BusinessException("Attribut dupliqué sur la variante");
            }
            VariantAttribute attribute = attributeRepository.findById(sel.getAttributeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attribut non trouvé: " + sel.getAttributeId()));
            VariantAttributeValue value = valueRepository.findById(sel.getValueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Valeur attribut non trouvée: " + sel.getValueId()));
            if (!value.getAttribute().getId().equals(attribute.getId())) {
                throw new BusinessException("Valeur invalide pour l'attribut " + attribute.getCode());
            }
            if (!Boolean.TRUE.equals(attribute.getIsActive()) || !Boolean.TRUE.equals(value.getIsActive())) {
                throw new BusinessException("Attribut ou valeur inactive");
            }
            variant.getAttributeValues().add(ProductVariantAttributeValue.builder()
                    .variant(variant)
                    .attribute(attribute)
                    .attributeValue(value)
                    .build());
            if (variant.getCouleur() == null && "COULEUR".equalsIgnoreCase(attribute.getCode())) {
                variant.setCouleur(value.getValue());
            }
            if (variant.getTaille() == null && "TAILLE".equalsIgnoreCase(attribute.getCode())) {
                variant.setTaille(value.getValue());
            }
        }
        assertUniqueCombination(variant);
        assertVariantUniqueness(variant);
    }

    @Transactional(readOnly = true)
    public void assertVariantUniqueness(ProductVariant variant) {
        assertUniqueLegacyCouleurTaille(variant);
        assertUniqueBarcode(variant);
        assertNoDuplicateProfile(variant);
    }

    private void assertUniqueLegacyCouleurTaille(ProductVariant variant) {
        if (variant.getAttributeValues() != null && !variant.getAttributeValues().isEmpty()) {
            return;
        }
        String couleur = normalizeText(variant.getCouleur());
        String taille = normalizeText(variant.getTaille());
        if (couleur == null && taille == null) {
            return;
        }
        for (ProductVariant other : variantRepository.findByProductId(variant.getProduct().getId())) {
            if (isSameVariant(variant, other)) {
                continue;
            }
            if (Objects.equals(couleur, normalizeText(other.getCouleur()))
                    && Objects.equals(taille, normalizeText(other.getTaille()))) {
                throw new BusinessException(
                        "Une variante avec la même couleur et la même taille existe déjà sur ce produit");
            }
        }
    }

    private void assertUniqueBarcode(ProductVariant variant) {
        if (variant.getCodeBarre() == null || variant.getCodeBarre().isBlank()) {
            return;
        }
        String code = variant.getCodeBarre().trim();
        barcodeRegistryService.assertAvailable(code, null, variant.getId(), null);
    }

    private void assertNoDuplicateProfile(ProductVariant variant) {
        String couleur = normalizeText(variant.getCouleur());
        String taille = normalizeText(variant.getTaille());
        java.math.BigDecimal prix = variant.getPrix();
        String sku = normalizeText(variant.getSku());
        String label = normalizeText(buildVariantLabel(variant));

        for (ProductVariant other : variantRepository.findByProductId(variant.getProduct().getId())) {
            if (isSameVariant(variant, other)) {
                continue;
            }
            if (sku != null && sku.equals(normalizeText(other.getSku()))) {
                throw new BusinessException("SKU variante déjà existant : " + sku);
            }
            boolean sameCouleur = Objects.equals(couleur, normalizeText(other.getCouleur()));
            boolean sameTaille = Objects.equals(taille, normalizeText(other.getTaille()));
            boolean samePrix = pricesEqual(prix, other.getPrix());
            boolean sameLabel = label != null && label.equals(normalizeText(buildVariantLabel(other)));

            if (sameCouleur && sameTaille && samePrix) {
                throw new BusinessException(
                        "Une variante identique existe déjà (même couleur, taille et prix)");
            }
            if (sameLabel && sameCouleur && sameTaille) {
                throw new BusinessException("Une variante avec le même libellé existe déjà sur ce produit");
            }
        }
    }

    private static boolean isSameVariant(ProductVariant a, ProductVariant b) {
        return a.getId() != null && a.getId().equals(b.getId());
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean pricesEqual(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    @Transactional(readOnly = true)
    public void assertUniqueCombination(ProductVariant variant) {
        if (variant.getAttributeValues().isEmpty()) {
            return;
        }
        List<Long> valueIds = variant.getAttributeValues().stream()
                .map(v -> v.getAttributeValue().getId())
                .sorted()
                .toList();
        long attributeCount = valueIds.size();
        List<Long> matches = variantAttributeValueRepository.findVariantIdsByExactValueCombination(
                variant.getProduct().getId(), valueIds, attributeCount);
        for (Long existingId : matches) {
            if (variant.getId() == null || !existingId.equals(variant.getId())) {
                throw new BusinessException("Combinaison d'attributs déjà existante sur ce produit");
            }
        }
    }

    @Transactional(readOnly = true)
    public String buildVariantLabel(ProductVariant variant) {
        if (variant.getAttributeValues() == null || variant.getAttributeValues().isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (variant.getCouleur() != null && !variant.getCouleur().isBlank()) {
                parts.add(variant.getCouleur());
            }
            if (variant.getTaille() != null && !variant.getTaille().isBlank()) {
                parts.add(variant.getTaille());
            }
            return parts.isEmpty() ? variant.getSku() : String.join(" ", parts);
        }
        return variant.getAttributeValues().stream()
                .sorted(Comparator.comparing(v -> v.getAttribute().getCode()))
                .map(v -> v.getAttributeValue().getValue())
                .collect(Collectors.joining(" "));
    }

    @Transactional
    public List<ProductVariant> generateVariants(Product product, ProductVariantGenerateRequest request) {
        if (request.getAttributeValues() == null || request.getAttributeValues().isEmpty()) {
            throw new BusinessException("Sélectionnez au moins un attribut avec des valeurs");
        }
        List<List<VariantAttributeSelectionRequest>> combinations = cartesian(request.getAttributeValues());
        List<ProductVariant> created = new ArrayList<>();
        int index = (int) variantRepository.countByProductId(product.getId()) + 1;
        for (List<VariantAttributeSelectionRequest> combo : combinations) {
            ProductVariantRequest vr = new ProductVariantRequest();
            vr.setAttributeSelections(combo);
            vr.setStock(0);
            created.add(buildDraftVariant(product, vr, index++));
        }
        return created;
    }

    private ProductVariant buildDraftVariant(Product product, ProductVariantRequest request, int index) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .stock(request.getStock() != null ? request.getStock() : 0)
                .build();
        applySelections(variant, resolveSelections(request));
        variant.setName(buildVariantLabel(variant));
        variant.setPrix(request.getPrix() != null ? request.getPrix() : product.getPrixVente());
        if (request.getSku() != null && !request.getSku().isBlank()) {
            variant.setSku(request.getSku().trim());
        } else {
            variant.setSku(buildVariantSku(product, variant, index));
        }
        return variant;
    }

    public String buildVariantSku(Product product, ProductVariant variant, int index) {
        List<String> parts = variant.getAttributeValues().stream()
                .sorted(Comparator.comparing(v -> v.getAttribute().getCode()))
                .map(v -> {
                    String code = v.getAttributeValue().getCode();
                    if (code != null && !code.isBlank()) {
                        return code;
                    }
                    return ProductSkuGenerator.normalizePart(v.getAttributeValue().getValue(), 8);
                })
                .filter(s -> !s.isEmpty())
                .toList();
        String suffix = parts.isEmpty()
                ? ProductSkuGenerator.variantSuffix(variant.getCouleur(), variant.getTaille(), index)
                : String.join("-", parts);
        String base = product.getSku() + "-" + suffix;
        return ProductSkuGenerator.ensureUnique(base, variantRepository::existsBySku);
    }

    private List<List<VariantAttributeSelectionRequest>> cartesian(Map<Long, List<Long>> attributeValues) {
        List<List<VariantAttributeSelectionRequest>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        List<Long> attributeIds = attributeValues.keySet().stream().sorted().toList();
        for (Long attributeId : attributeIds) {
            List<Long> valueIds = attributeValues.get(attributeId);
            if (valueIds == null || valueIds.isEmpty()) {
                throw new BusinessException("Chaque attribut doit avoir au moins une valeur");
            }
            attributeRepository.findById(attributeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Attribut non trouvé: " + attributeId));
            List<List<VariantAttributeSelectionRequest>> next = new ArrayList<>();
            for (List<VariantAttributeSelectionRequest> partial : result) {
                for (Long valueId : valueIds) {
                    List<VariantAttributeSelectionRequest> copy = new ArrayList<>(partial);
                    VariantAttributeSelectionRequest sel = new VariantAttributeSelectionRequest();
                    sel.setAttributeId(attributeId);
                    sel.setValueId(valueId);
                    copy.add(sel);
                    next.add(copy);
                }
            }
            result = next;
        }
        return result;
    }

    private VariantAttributeSelectionRequest selectionForLegacyValue(String attributeCode, String rawValue) {
        VariantAttribute attribute = attributeRepository.findByCode(attributeCode)
                .orElseGet(() -> attributeRepository.save(VariantAttribute.builder()
                        .name("COULEUR".equalsIgnoreCase(attributeCode) ? "Couleur"
                                : "TAILLE".equalsIgnoreCase(attributeCode) ? "Taille" : attributeCode)
                        .code(attributeCode.toUpperCase(java.util.Locale.ROOT))
                        .isActive(true)
                        .build()));
        VariantAttributeValue value = valueRepository.findByAttributeIdAndValueIgnoreCase(attribute.getId(), rawValue)
                .orElseGet(() -> valueRepository.save(VariantAttributeValue.builder()
                        .attribute(attribute)
                        .value(rawValue)
                        .code(ProductSkuGenerator.normalizePart(rawValue, 8))
                        .sortOrder(0)
                        .isActive(true)
                        .build()));
        VariantAttributeSelectionRequest sel = new VariantAttributeSelectionRequest();
        sel.setAttributeId(attribute.getId());
        sel.setValueId(value.getId());
        return sel;
    }
}
