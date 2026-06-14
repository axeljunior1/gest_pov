package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductVariantRequest {

    private String couleur;
    private String taille;

    private String sku;

    private BigDecimal prix;

    private BigDecimal costPrice;

    private Boolean sellable;

    private Boolean stockable;

    private Boolean active;

    private Integer stock;

    /** Sélections d'attributs dynamiques (prioritaire sur couleur/taille legacy). */
    private List<VariantAttributeSelectionRequest> attributeSelections;

    private String codeBarre;
    private BarcodeType barcodeType;
    private Boolean generateBarcode;
}
