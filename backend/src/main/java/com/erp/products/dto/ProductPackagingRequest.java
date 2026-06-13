package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductPackagingRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String symbole;

    @NotNull(message = "La quantité en unité de base est obligatoire")
    @Positive(message = "La quantité doit être strictement positive")
    private BigDecimal quantiteBase;

    private String codeBarre;

    /** Prix de vente pour 1 conditionnement. Si absent, défaut = prix unité produit × quantiteBase. */
    private BigDecimal prixVente;

    private BigDecimal prixAchat;

    private Boolean defaultVente;

    private Boolean defaultAchat;

    private Boolean actif;

    /** Alias legacy : achat principal */
    private Boolean principal;
}
