package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PosCatalogResponse {

    private List<CategoryResponse> categories;
    private List<PosProductResponse> products;
    private List<PosProductResponse> favorites;
    private List<PosProductResponse> promotions;
    private List<PosProductResponse> recent;
    private List<PosProductResponse> topSales;
}
