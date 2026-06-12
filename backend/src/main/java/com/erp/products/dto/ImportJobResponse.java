package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ImportJobResponse {

    private Long id;
    private String importType;
    private String status;
    private String fileName;
    private String createdBy;
    private int totalRows;
    private int successRows;
    private int errorRows;
    private String errorReport;
    private Instant createdAt;
    private Instant completedAt;
}
