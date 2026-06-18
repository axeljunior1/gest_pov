package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoDataStatusResponse {

    private boolean demoPresent;
    private boolean demoAutoEnabled;
    private boolean demoManualEnabled;
    private boolean setupCompleted;
    private int demoProducts;
    private int demoCategories;
    private int demoCustomers;
    private int demoSuppliers;
    private int demoSales;
    private int demoStockMovements;
    private String markerSku;
    private String message;
}
