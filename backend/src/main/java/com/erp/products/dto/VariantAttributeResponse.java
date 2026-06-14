package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class VariantAttributeResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
    private List<VariantAttributeValueResponse> values;
    private Instant createdAt;
    private Instant updatedAt;
}
