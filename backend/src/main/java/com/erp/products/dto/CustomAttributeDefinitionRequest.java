package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomAttributeDefinitionRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String label;

    @NotBlank
    private String type;
}
