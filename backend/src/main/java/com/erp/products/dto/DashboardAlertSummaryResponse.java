package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardAlertSummaryResponse {

    private long openAlerts;
    private long openLowStock;
    private long openOutOfStock;
    private long openExpirySoon;
    private long openExpired;
}
