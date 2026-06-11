package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UnitOfMeasureResponse {

    private Long id;
    private String nom;
    private String symbole;
    private Instant createdAt;
}
