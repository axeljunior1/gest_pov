package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashDifferenceReasonOption {
    private String code;
    private String label;
}
