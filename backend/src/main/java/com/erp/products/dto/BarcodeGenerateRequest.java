package com.erp.products.dto;

import com.erp.products.domain.enums.BarcodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BarcodeGenerateRequest {

    @NotBlank
    private String content;

    @NotNull
    private BarcodeType type;
}
