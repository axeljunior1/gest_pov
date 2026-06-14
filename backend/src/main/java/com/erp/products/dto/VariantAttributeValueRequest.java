package com.erp.products.dto;

import lombok.Data;

@Data
public class VariantAttributeValueRequest {

    private String value;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
}
