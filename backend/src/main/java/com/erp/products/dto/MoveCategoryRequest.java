package com.erp.products.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCategoryRequest {

    @NotNull
    private Long categorieId;

    private String utilisateur;
}
