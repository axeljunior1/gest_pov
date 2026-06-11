package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String couleur;
    private String taille;
    private String sku;
    private BigDecimal prix;
    private Integer stock;
    private String codeBarre;
    private BarcodeType barcodeType;
    private String barcodeImageBase64;
    private Instant createdAt;
    private Instant updatedAt;
}
