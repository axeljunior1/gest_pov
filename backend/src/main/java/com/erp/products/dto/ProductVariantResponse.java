package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String couleur;
    private String taille;
    private String name;
    private String label;
    private String sku;
    private List<VariantAttributeSelectionResponse> attributeSelections;
    private BigDecimal prix;
    private BigDecimal costPrice;
    private Boolean sellable;
    private Boolean stockable;
    private Boolean active;
    private Integer stock;
    private String codeBarre;
    private BarcodeType barcodeType;
    private String barcodeImageBase64;
    private Instant createdAt;
    private Instant updatedAt;
}
