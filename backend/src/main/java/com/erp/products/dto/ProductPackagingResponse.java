package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductPackagingResponse {

    private Long id;
    private Long productId;
    private Long variantId;
    private String variantLabel;
    private String nom;
    private String symbole;
    private BigDecimal quantiteBase;
    private String codeBarre;
    private BigDecimal prixVente;
    private BigDecimal prixAchat;
    private Boolean defaultVente;
    private Boolean defaultAchat;
    private Boolean actif;
    private Boolean principal;
    private String baseUnitSymbole;
    private String baseUnitNom;
    private Instant createdAt;
    private Instant updatedAt;
}
