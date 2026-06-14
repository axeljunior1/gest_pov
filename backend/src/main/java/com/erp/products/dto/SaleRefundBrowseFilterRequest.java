package com.erp.products.dto;

import com.erp.products.domain.enums.SaleRefundStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class SaleRefundBrowseFilterRequest {

    private String q;
    private SaleRefundStatus status;
    private Long saleId;
    private Instant dateFrom;
    private Instant dateTo;
    private Integer page;
    private Integer limit;
}
