package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductPackagingResponse {

    private Long id;
    private Long productId;
    private String nom;
    private String symbole;
    private BigDecimal quantiteBase;
    private String codeBarre;
    private Boolean principal;
    private String baseUnitSymbole;
    private String baseUnitNom;
    private Instant createdAt;
    private Instant updatedAt;
}
