package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsCategoriesResponse {

    private List<AnalyticsCategoryRow> items;
}
