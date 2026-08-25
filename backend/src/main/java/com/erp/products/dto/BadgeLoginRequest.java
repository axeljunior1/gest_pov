package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BadgeLoginRequest {

    @NotBlank
    private String badgeCode;

    @NotBlank
    private String pin;
}
