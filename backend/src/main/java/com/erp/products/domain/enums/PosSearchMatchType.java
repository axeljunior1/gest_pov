package com.erp.products.domain.enums;

public enum PosSearchMatchType {
    EXACT_BARCODE,
    EXACT_PACKAGING_BARCODE,
    EXACT_SKU,
    BARCODE_NOT_FOUND,
    BARCODE_AMBIGUOUS,
    TEXT,
    NONE
}
