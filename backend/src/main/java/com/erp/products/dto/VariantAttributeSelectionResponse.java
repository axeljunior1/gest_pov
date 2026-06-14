package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VariantAttributeSelectionResponse {

    private Long attributeId;
    private String attributeCode;
    private String attributeName;
    private Long valueId;
    private String value;
    private String valueCode;
}
