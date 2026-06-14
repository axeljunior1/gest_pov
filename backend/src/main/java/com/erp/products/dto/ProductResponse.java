package com.erp.products.dto;

import com.erp.products.domain.enums.DocumentType;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String nom;
    private String sku;
    private String codeBarre;
    private String description;
    private String marque;
    private Long categorieId;
    private String categorieNom;
    private String categoriePath;
    private BigDecimal prixAchat;
    private BigDecimal prixVente;
    private BigDecimal prixPromotionnel;
    private Instant prixPromotionnelDebut;
    private Instant prixPromotionnelFin;
    private Long fournisseurPrincipalId;
    private String fournisseurPrincipalNom;
    private Long unitId;
    private String unitSymbole;
    private String baseUnitNom;
    private String baseUnitSymbole;
    private ProductStatus statut;
    private LifecycleStatus cycleVie;
    private Boolean sellable;
    private Boolean hasVariants;
    private Boolean stockable;
    private Integer stockTotal;
    private List<ProductVariantResponse> variantes;
    private List<ProductPackagingResponse> conditionnements;
    private List<ProductSupplierResponse> fournisseurs;
    private List<ProductImageResponse> images;
    private List<ProductDocumentResponse> documents;
    private Map<String, String> attributs;
    private Instant createdAt;
    private Instant updatedAt;
}
