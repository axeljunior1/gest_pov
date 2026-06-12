package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockOperationRequest {

    @NotNull
    private Long productId;

    private Long variantId;

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long locationId;

    private Long lotId;

    /** Quantité en unité de base (peut être négative pour ajustement). */
    private BigDecimal quantityBase;

    /** Saisie en conditionnement — convertie en unité de base. */
    private Long packagingId;

    @Positive
    private BigDecimal packagingQuantity;

    private String referenceType;
    private String reference;
    private String reason;

    private String utilisateur = "system";
}
