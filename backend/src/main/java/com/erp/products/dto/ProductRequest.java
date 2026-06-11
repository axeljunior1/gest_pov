package com.erp.products.dto;

import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class ProductRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le SKU est obligatoire")
    private String sku;

    private String description;
    private String marque;
    private Long categorieId;
    private BigDecimal prixAchat;
    private BigDecimal prixVente;
    private BigDecimal prixPromotionnel;
    private Instant prixPromotionnelDebut;
    private Instant prixPromotionnelFin;
    private Long fournisseurPrincipalId;
    private Long unitId;
    private ProductStatus statut;
    private LifecycleStatus cycleVie;
    private List<ProductVariantRequest> variantes;
    private List<ProductSupplierRequest> fournisseurs;
    private Map<String, String> attributs;
    private String utilisateur;
}
