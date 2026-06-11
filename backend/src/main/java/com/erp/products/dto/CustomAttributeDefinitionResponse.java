package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CustomAttributeDefinitionResponse {

    private Long id;
    private String code;
    private String label;
    private String type;
    private Instant createdAt;
}
