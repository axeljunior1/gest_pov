package com.erp.products.mapper;

import com.erp.products.domain.entity.*;
import com.erp.products.dto.*;
import com.erp.products.service.BarcodeService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public CategoryResponse toCategoryResponse(Category category, boolean withChildren) {
        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .id(category.getId())
                .nom(category.getNom())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt());

        if (category.getParent() != null) {
            builder.parentId(category.getParent().getId())
                    .parentNom(category.getParent().getNom());
        }

        if (withChildren && category.getChildren() != null) {
            builder.children(category.getChildren().stream()
                    .map(c -> toCategoryResponse(c, true))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public SupplierResponse toSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .nom(supplier.getNom())
                .email(supplier.getEmail())
                .telephone(supplier.getTelephone())
                .adresse(supplier.getAdresse())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    public UnitOfMeasureResponse toUnitResponse(UnitOfMeasure unit) {
        return UnitOfMeasureResponse.builder()
                .id(unit.getId())
                .nom(unit.getNom())
                .symbole(unit.getSymbole())
                .createdAt(unit.getCreatedAt())
                .build();
    }

    public UnitConversionResponse toConversionResponse(UnitConversion conversion) {
        return UnitConversionResponse.builder()
                .id(conversion.getId())
                .fromUnitId(conversion.getFromUnit().getId())
                .fromUnitSymbole(conversion.getFromUnit().getSymbole())
                .toUnitId(conversion.getToUnit().getId())
                .toUnitSymbole(conversion.getToUnit().getSymbole())
                .factor(conversion.getFactor())
                .build();
    }

    public ProductPackagingResponse toPackagingResponse(ProductPackaging packaging, Product product) {
        ProductPackagingResponse.ProductPackagingResponseBuilder builder = ProductPackagingResponse.builder()
                .id(packaging.getId())
                .productId(product.getId())
                .nom(packaging.getNom())
                .symbole(packaging.getSymbole())
                .quantiteBase(packaging.getQuantiteBase())
                .codeBarre(packaging.getCodeBarre())
                .principal(packaging.getPrincipal())
                .createdAt(packaging.getCreatedAt())
                .updatedAt(packaging.getUpdatedAt());

        if (product.getUnit() != null) {
            builder.baseUnitSymbole(product.getUnit().getSymbole())
                    .baseUnitNom(product.getUnit().getNom());
        }
        return builder.build();
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant, BarcodeService barcodeService) {
        ProductVariantResponse.ProductVariantResponseBuilder builder = ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .couleur(variant.getCouleur())
                .taille(variant.getTaille())
                .sku(variant.getSku())
                .prix(variant.getPrix())
                .stock(variant.getStock())
                .codeBarre(variant.getCodeBarre())
                .barcodeType(variant.getBarcodeType())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt());

        if (variant.getCodeBarre() != null && variant.getBarcodeType() != null && barcodeService != null) {
            try {
                builder.barcodeImageBase64(barcodeService.generateBase64(variant.getCodeBarre(), variant.getBarcodeType()));
            } catch (Exception ignored) {
                // barcode preview optional
            }
        }

        return builder.build();
    }

    public ProductSupplierResponse toProductSupplierResponse(ProductSupplier ps) {
        return ProductSupplierResponse.builder()
                .id(ps.getId())
                .supplierId(ps.getSupplier().getId())
                .supplierNom(ps.getSupplier().getNom())
                .principal(ps.getPrincipal())
                .referenceFournisseur(ps.getReferenceFournisseur())
                .delaiLivraisonJours(ps.getDelaiLivraisonJours())
                .prixNegocie(ps.getPrixNegocie())
                .build();
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .fileName(image.getFileName())
                .url("/uploads/" + image.getFilePath())
                .principale(image.getPrincipale())
                .ordre(image.getOrdre())
                .createdAt(image.getCreatedAt())
                .build();
    }

    public ProductDocumentResponse toDocumentResponse(ProductDocument doc) {
        return ProductDocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .url("/uploads/" + doc.getFilePath())
                .type(doc.getType())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public ProductResponse toProductResponse(Product product, BarcodeService barcodeService, boolean full) {
        int stockTotal = product.getVariantes().stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();

        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .nom(product.getNom())
                .sku(product.getSku())
                .description(product.getDescription())
                .marque(product.getMarque())
                .prixAchat(product.getPrixAchat())
                .prixVente(product.getPrixVente())
                .prixPromotionnel(product.getPrixPromotionnel())
                .prixPromotionnelDebut(product.getPrixPromotionnelDebut())
                .prixPromotionnelFin(product.getPrixPromotionnelFin())
                .statut(product.getStatut())
                .cycleVie(product.getCycleVie())
                .stockTotal(stockTotal)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        if (product.getCategorie() != null) {
            builder.categorieId(product.getCategorie().getId())
                    .categorieNom(product.getCategorie().getNom())
                    .categoriePath(buildCategoryPath(product.getCategorie()));
        }

        if (product.getFournisseurPrincipal() != null) {
            builder.fournisseurPrincipalId(product.getFournisseurPrincipal().getId())
                    .fournisseurPrincipalNom(product.getFournisseurPrincipal().getNom());
        }

        if (product.getUnit() != null) {
            builder.unitId(product.getUnit().getId())
                    .unitSymbole(product.getUnit().getSymbole())
                    .baseUnitNom(product.getUnit().getNom())
                    .baseUnitSymbole(product.getUnit().getSymbole());
        }

        if (full) {
            builder.variantes(product.getVariantes().stream()
                    .map(v -> toVariantResponse(v, barcodeService))
                    .collect(Collectors.toList()));
            builder.fournisseurs(product.getFournisseurs().stream()
                    .map(this::toProductSupplierResponse)
                    .collect(Collectors.toList()));
            builder.images(product.getImages().stream()
                    .map(this::toImageResponse)
                    .collect(Collectors.toList()));
            builder.documents(product.getDocuments().stream()
                    .map(this::toDocumentResponse)
                    .collect(Collectors.toList()));

            Map<String, String> attributs = new LinkedHashMap<>();
            product.getAttributs().forEach(a -> attributs.put(a.getAttribute().getCode(), a.getValeur()));
            builder.attributs(attributs);

            builder.conditionnements(product.getConditionnements().stream()
                    .map(p -> toPackagingResponse(p, product))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public PriceHistoryResponse toPriceHistoryResponse(PriceHistory history) {
        return PriceHistoryResponse.builder()
                .id(history.getId())
                .productId(history.getProduct() != null ? history.getProduct().getId() : null)
                .variantId(history.getVariant() != null ? history.getVariant().getId() : null)
                .type(history.getType())
                .ancienPrix(history.getAncienPrix())
                .nouveauPrix(history.getNouveauPrix())
                .utilisateur(history.getUtilisateur())
                .dateModification(history.getDateModification())
                .build();
    }

    public AuditLogResponse toAuditResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .details(log.getDetails())
                .utilisateur(log.getUtilisateur())
                .dateAction(log.getDateAction())
                .build();
    }

    public CustomAttributeDefinitionResponse toAttributeDefinitionResponse(CustomAttributeDefinition def) {
        return CustomAttributeDefinitionResponse.builder()
                .id(def.getId())
                .code(def.getCode())
                .label(def.getLabel())
                .type(def.getType())
                .createdAt(def.getCreatedAt())
                .build();
    }

    private String buildCategoryPath(Category category) {
        StringBuilder path = new StringBuilder(category.getNom());
        Category current = category.getParent();
        while (current != null) {
            path.insert(0, current.getNom() + " > ");
            current = current.getParent();
        }
        return path.toString();
    }
}
