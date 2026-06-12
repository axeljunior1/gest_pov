package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarehouseRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String nom;
    private String adresse;
    private Boolean actif = true;
}
