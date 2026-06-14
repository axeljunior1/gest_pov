package com.erp.products.dto;

import lombok.Data;

@Data
public class LifecycleRejectRequest {
    private String reason;
    private String utilisateur;
}
