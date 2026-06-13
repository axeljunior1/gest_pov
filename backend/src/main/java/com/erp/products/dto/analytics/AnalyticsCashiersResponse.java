package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsCashiersResponse {

    private List<AnalyticsCashierRow> items;
}
