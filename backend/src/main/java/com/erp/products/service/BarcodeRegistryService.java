package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BarcodeRegistryService {

    public static final String MSG_DUPLICATE = "Ce code-barres est déjà utilisé";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;

    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean equalsNormalized(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) {
            return false;
        }
        return na.equalsIgnoreCase(nb);
    }

    @Transactional(readOnly = true)
    public boolean isTaken(String code) {
        return !findConflicts(code, null, null, null).isEmpty();
    }

    @Transactional(readOnly = true)
    public void assertAvailable(String code, Long excludeProductId, Long excludeVariantId, Long excludePackagingId) {
        List<String> conflicts = findConflicts(code, excludeProductId, excludeVariantId, excludePackagingId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(MSG_DUPLICATE + " : " + normalize(code));
        }
    }

    @Transactional(readOnly = true)
    public List<String> findConflicts(
            String code,
            Long excludeProductId,
            Long excludeVariantId,
            Long excludePackagingId) {
        String normalized = normalize(code);
        if (normalized == null) {
            return List.of();
        }
        List<String> conflicts = new ArrayList<>();

        productRepository.findByCodeBarreNormalized(normalized).ifPresent(product -> {
            if (!Objects.equals(product.getId(), excludeProductId)) {
                conflicts.add("produit " + product.getSku());
            }
        });

        variantRepository.findByCodeBarreNormalized(normalized).ifPresent(variant -> {
            if (!Objects.equals(variant.getId(), excludeVariantId)) {
                conflicts.add("variante " + variant.getSku());
            }
        });

        packagingRepository.findByCodeBarreNormalized(normalized).ifPresent(packaging -> {
            if (!Objects.equals(packaging.getId(), excludePackagingId)) {
                conflicts.add("conditionnement " + packaging.getNom());
            }
        });

        return conflicts;
    }

    @Transactional(readOnly = true)
    public int countActiveMatches(String code) {
        String normalized = normalize(code);
        if (normalized == null) {
            return 0;
        }
        int count = 0;
        if (productRepository.findActiveSimpleByCodeBarre(normalized).isPresent()) {
            count++;
        }
        if (variantRepository.findActiveSellableByCodeBarre(normalized).isPresent()) {
            count++;
        }
        if (packagingRepository.findActiveByCodeBarre(normalized).isPresent()) {
            count++;
        }
        return count;
    }
}
