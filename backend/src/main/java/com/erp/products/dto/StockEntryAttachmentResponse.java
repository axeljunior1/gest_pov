package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StockEntryAttachmentResponse {
    private Long id;
    private String fileName;
    private String url;
    private Instant createdAt;
}
