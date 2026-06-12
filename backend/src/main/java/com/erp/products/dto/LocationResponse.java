package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseCode;
    private String code;
    private String nom;
    private Boolean actif;
}
