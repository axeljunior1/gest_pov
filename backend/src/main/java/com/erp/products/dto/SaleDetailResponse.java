package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SaleDetailResponse {

    private SaleResponse sale;
    private BigDecimal totalRefunded;
    private List<SaleRefundSummaryResponse> refunds;
    private List<SaleEventResponse> timeline;
}
