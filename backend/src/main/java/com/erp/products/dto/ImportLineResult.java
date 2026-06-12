package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportLineResult {

    private int lineNumber;
    private int dataRowIndex;
    private String status;
    private String action;
    private String identifier;
    private String message;
}
