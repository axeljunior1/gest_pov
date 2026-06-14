package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PosVariantResponse {

    private Long id;
    private String label;
    private String sku;
    private BigDecimal unitPrice;
    private BigDecimal stockAvailable;
    private boolean outOfStock;
    private boolean lowStock;
    private String codeBarre;
    private List<VariantAttributeSelectionResponse> attributes;
    private List<PosPackagingResponse> packagings;
}
