package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportPreviewResponse {

    private int totalRows;
    private int validRows;
    private int errorRows;
    private List<ImportLineResult> lines;
}
