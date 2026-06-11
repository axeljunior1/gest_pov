package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PackagingToBaseResponse {

    private Long productId;
    private Long packagingId;
    private String packagingNom;
    private BigDecimal quantityPackaging;
    private BigDecimal quantiteBase;
    private String baseUnitSymbole;
    private String baseUnitNom;
    private String explanation;
}
