package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsPagedProductsResponse {

    private List<AnalyticsProductRow> items;
    private long totalElements;
    private int page;
    private int size;
}
