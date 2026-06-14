package com.erp.products.dto;

import com.erp.products.domain.enums.PosSearchMatchType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PosSearchResultResponse {

    private PosSearchMatchType matchType;
    private List<PosProductResponse> products;
    private String message;
    private Boolean ambiguous;
    private List<BarcodeLookupResult> barcodeMatches;
}
