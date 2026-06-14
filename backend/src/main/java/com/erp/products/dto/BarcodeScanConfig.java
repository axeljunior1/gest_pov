package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BarcodeScanConfig {

    private boolean scanEnabled;
    private int minLength;
    private boolean autoAddToCart;
    private String searchPriority;
}
