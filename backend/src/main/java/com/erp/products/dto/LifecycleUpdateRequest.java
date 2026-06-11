package com.erp.products.dto;

import com.erp.products.domain.enums.LifecycleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LifecycleUpdateRequest {

    @NotNull
    private LifecycleStatus cycleVie;

    private String utilisateur;
}
