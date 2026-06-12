package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LotResponse {
    private Long id;
    private Long productId;
    private Long variantId;
    private String numeroLot;
    private LocalDate datePeremption;
    private LocalDate dateFabrication;
}
