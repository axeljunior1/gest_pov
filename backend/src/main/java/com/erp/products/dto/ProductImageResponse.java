package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProductImageResponse {

    private Long id;
    private String fileName;
    private String url;
    private Boolean principale;
    private Integer ordre;
    private Instant createdAt;
}
