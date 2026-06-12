package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NumberingConfigResponse {

    private String entryPrefix;
    private String exitPrefix;
    private String inventoryPrefix;
    private String movementPrefix;
}
