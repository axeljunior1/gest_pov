package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PosPackagingResponse {

    private Long id;
    private String nom;
    private BigDecimal quantiteBase;
    private BigDecimal salePrice;
    private String codeBarre;
    private boolean defaultSale;
    private boolean active;
}
