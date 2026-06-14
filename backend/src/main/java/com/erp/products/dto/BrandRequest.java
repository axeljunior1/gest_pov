package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;
}
