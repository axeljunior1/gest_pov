package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BarcodeScanResponse {

    private SaleResponse sale;
    private BarcodeLookupResult lookup;
    private String message;
}
