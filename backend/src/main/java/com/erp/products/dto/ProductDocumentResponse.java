package com.erp.products.dto;

import com.erp.products.domain.enums.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProductDocumentResponse {

    private Long id;
    private String fileName;
    private String url;
    private DocumentType type;
    private Instant createdAt;
}
