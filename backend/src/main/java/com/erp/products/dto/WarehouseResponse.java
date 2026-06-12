package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WarehouseResponse {
    private Long id;
    private String code;
    private String nom;
    private String adresse;
    private Boolean actif;
    private Instant createdAt;
}
