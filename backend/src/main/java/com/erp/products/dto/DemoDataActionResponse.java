package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoDataActionResponse {

    private String status;
    private String message;
    private int products;
    private int customers;
    private int sales;
    private int categoriesRemoved;
    private int suppliersRemoved;
}
