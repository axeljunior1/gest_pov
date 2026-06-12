package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportValidateResponse {

    private ImportJobResponse job;
    private List<ImportLineResult> lines;
}
