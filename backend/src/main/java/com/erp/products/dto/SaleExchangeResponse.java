package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SaleExchangeResponse {
    private Long id;
    private String exchangeNumber;
    private Long originalSaleId;
    private String originalSaleNumber;
    private Long refundId;
    private String refundNumber;
    private BigDecimal returnTotal;
    private Long newSaleId;
    private String newSaleNumber;
    private BigDecimal newItemsTotal;
    /** Positif = paye par le client, negatif = rembourse au client, zero = echange pur. */
    private BigDecimal netAmount;
    /** Part de la reprise compensee par le nouvel article (jamais un vrai paiement). */
    private BigDecimal offsetAmount;
    private String createdBy;
    private Instant createdAt;
}
