package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeLookupType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BarcodeLookupResult {

    private BarcodeLookupType type;
    private Long productId;
    private Long variantId;
    private Long packagingId;
    private String displayName;
    private BigDecimal salePrice;
    private BigDecimal quantityInBaseUnit;
    private String barcode;
}
