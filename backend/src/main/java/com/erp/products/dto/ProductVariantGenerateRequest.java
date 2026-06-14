package com.erp.products.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductVariantGenerateRequest {

    /** attributeId → liste de valueIds pour produit cartésien */
    private Map<Long, List<Long>> attributeValues;
}
