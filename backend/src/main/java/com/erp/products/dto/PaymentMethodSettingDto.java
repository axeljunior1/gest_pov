package com.erp.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodSettingDto {

    @NotBlank
    @Size(max = 32)
    private String code;
    @NotBlank
    @Size(max = 80)
    private String label;
    private boolean enabled;
}
