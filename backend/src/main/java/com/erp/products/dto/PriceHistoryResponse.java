package com.erp.products.dto;

import com.erp.products.domain.enums.PriceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PriceHistoryResponse {

    private Long id;
    private Long productId;
    private Long variantId;
    private PriceType type;
    private BigDecimal ancienPrix;
    private BigDecimal nouveauPrix;
    private String utilisateur;
    private Instant dateModification;
}
