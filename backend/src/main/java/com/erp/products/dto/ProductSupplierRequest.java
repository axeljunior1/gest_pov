package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSupplierRequest {

    @NotNull
    private Long supplierId;

    private Boolean principal;
    private String referenceFournisseur;
    private Integer delaiLivraisonJours;
    private BigDecimal prixNegocie;
}
