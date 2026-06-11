package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SupplierResponse {

    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private Instant createdAt;
    private Instant updatedAt;
}
