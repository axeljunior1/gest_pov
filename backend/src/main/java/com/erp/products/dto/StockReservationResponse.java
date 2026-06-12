package com.erp.products.dto;

import com.erp.products.domain.enums.StockReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockReservationResponse {
    private Long id;
    private Long productId;
    private Long variantId;
    private Long warehouseId;
    private Long locationId;
    private Long lotId;
    private BigDecimal quantity;
    private StockReservationStatus status;
    private String reference;
    private String utilisateur;
    private Instant createdAt;
}
