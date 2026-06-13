package com.erp.products.dto;

import com.erp.products.domain.enums.ReferenceValueCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferenceValueResponse {

    private Long id;
    private ReferenceValueCategory category;
    private String code;
    private String label;
    private Integer sortOrder;
}
