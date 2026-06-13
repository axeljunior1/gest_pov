package com.erp.products.controller;

import com.erp.products.dto.analytics.*;
import com.erp.products.service.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsDashboardService dashboardService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final ProductAnalyticsService productAnalyticsService;
    private final PaymentAnalyticsService paymentAnalyticsService;
    private final CashierAnalyticsService cashierAnalyticsService;
    private final StockAnalyticsService stockAnalyticsService;
    private final AnalyticsExportService exportService;

    @GetMapping("/overview")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsOverviewResponse overview(AnalyticsFilterRequest filter) {
        return dashboardService.getOverview(filter);
    }

    @GetMapping("/sales/timeline")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsTimelineResponse timeline(AnalyticsFilterRequest filter) {
        return salesAnalyticsService.getTimeline(filter);
    }

    @GetMapping("/products/top")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsPagedProductsResponse topProducts(AnalyticsFilterRequest filter) {
        return productAnalyticsService.getTopProducts(filter);
    }

    @GetMapping("/categories")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsCategoriesResponse categories(AnalyticsFilterRequest filter) {
        return productAnalyticsService.getCategories(filter);
    }

    @GetMapping("/payments")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsPaymentsResponse payments(AnalyticsFilterRequest filter) {
        return paymentAnalyticsService.getPayments(filter);
    }

    @GetMapping("/cashiers")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.cashier.read')")
    public AnalyticsCashiersResponse cashiers(AnalyticsFilterRequest filter) {
        return cashierAnalyticsService.getCashiers(filter);
    }

    @GetMapping("/customers")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read')")
    public AnalyticsCustomerSummaryResponse customers(AnalyticsFilterRequest filter) {
        return cashierAnalyticsService.getCustomers(filter);
    }

    @GetMapping("/stock-alerts")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.stock.read')")
    public AnalyticsStockAlertsResponse stockAlerts(AnalyticsFilterRequest filter) {
        return stockAnalyticsService.getStockAlerts(filter);
    }

    @GetMapping("/business-alerts")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'analytics.read', 'analytics.sales.read', 'analytics.stock.read')")
    public AnalyticsBusinessAlertsResponse businessAlerts(AnalyticsFilterRequest filter) {
        return stockAnalyticsService.getBusinessAlerts(filter);
    }

    @GetMapping("/export")
    @PreAuthorize("@permissionChecker.has(authentication, 'analytics.export')")
    public ResponseEntity<byte[]> export(
            AnalyticsFilterRequest filter,
            @RequestParam(defaultValue = "products") String type) {
        byte[] data = exportService.exportCsv(filter, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analytics-" + type + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(data);
    }
}
