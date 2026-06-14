package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PosProductResponse {

    private Long id;
    private String nom;
    private String sku;
    private Long categoryId;
    private String categoryNom;
    private BigDecimal unitPrice;
    private boolean promotional;
    private BigDecimal stockAvailable;
    private boolean outOfStock;
    private boolean lowStock;
    private boolean sellable;
    private boolean hasVariants;
    private boolean stockable;
    private boolean requiresVariantSelection;
    private List<PosVariantResponse> variants;
    private String imageUrl;
    private List<String> barcodes;
    private List<PosPackagingResponse> packagings;
    /** Renseigné lors d'un scan code-barres conditionnement. */
    private Long matchedPackagingId;
    /** Renseigné lors d'un scan code-barres / SKU variante. */
    private Long matchedVariantId;
}
