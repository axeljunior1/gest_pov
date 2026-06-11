package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {

    private String couleur;
    private String taille;

    @NotBlank(message = "Le SKU est obligatoire")
    private String sku;

    private BigDecimal prix;

    @NotNull
    private Integer stock;

    private String codeBarre;
    private BarcodeType barcodeType;
    private Boolean generateBarcode;
}
