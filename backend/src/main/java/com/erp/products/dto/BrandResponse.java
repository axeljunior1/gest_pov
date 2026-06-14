package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BrandResponse {

    private Long id;
    private String nom;
    private Instant createdAt;
    private Instant updatedAt;
}
