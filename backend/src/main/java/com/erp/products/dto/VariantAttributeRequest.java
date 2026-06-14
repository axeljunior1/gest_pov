package com.erp.products.dto;

import lombok.Data;

import java.util.List;

@Data
public class VariantAttributeRequest {

    private String name;
    private String code;
    private String description;
    private Boolean isActive;
    private List<VariantAttributeValueRequest> values;
}
