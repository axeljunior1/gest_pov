package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSupplierResponse {

    private Long id;
    private Long supplierId;
    private String supplierNom;
    private Boolean principal;
    private String referenceFournisseur;
    private Integer delaiLivraisonJours;
    private BigDecimal prixNegocie;
}
