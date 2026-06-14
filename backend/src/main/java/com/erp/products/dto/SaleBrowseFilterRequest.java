package com.erp.products.dto;

import com.erp.products.domain.enums.SaleStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class SaleBrowseFilterRequest {

    private String q;
    private SaleStatus status;
    private Instant dateFrom;
    private Instant dateTo;
    private Integer page;
    private Integer limit;
}
