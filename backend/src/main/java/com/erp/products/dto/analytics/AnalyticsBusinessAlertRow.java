package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalyticsBusinessAlertRow {

    private String code;
    private String severity;
    private String title;
    private String message;
    private BigDecimal value;
    private String entityType;
    private Long entityId;
}
