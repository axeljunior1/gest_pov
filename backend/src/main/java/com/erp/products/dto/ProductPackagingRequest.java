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

    private Boolean principal;
}
