package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class VariantAttributeValueResponse {

    private Long id;
    private Long attributeId;
    private String attributeCode;
    private String attributeName;
    private String value;
    private String code;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
