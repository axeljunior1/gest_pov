package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnitOfMeasureRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le symbole est obligatoire")
    private String symbole;
}
