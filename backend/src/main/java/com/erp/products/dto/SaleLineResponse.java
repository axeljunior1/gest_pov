package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SaleLineResponse {

    private Long id;
    private Long productId;
    private String productNom;
    private String productSku;
    private String variantNameSnapshot;
    private Long variantId;
    private Long packagingId;
    private String packagingNameSnapshot;
    private BigDecimal packagingQuantitySnapshot;
    private BigDecimal quantityInput;
    private BigDecimal quantityInBaseUnit;
    private BigDecimal unitPrice;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal discountAmount;
    private BigDecimal taxRate;
    private BigDecimal lineTotal;
    /** Stock disponible (unité de base) dans l'entrepôt de la vente. */
    private BigDecimal stockAvailable;
    /** True si la quantité en panier dépasse le stock disponible. */
    private Boolean stockInsufficient;
}
