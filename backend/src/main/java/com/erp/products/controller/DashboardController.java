package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public DashboardStockSummaryResponse summary() {
        return dashboardService.getStockSummary();
    }

    @GetMapping("/alerts")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public DashboardAlertSummaryResponse alerts() {
        return dashboardService.getAlertSummary();
    }

    @GetMapping("/movements/recent")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public List<StockMovementResponse> recentMovements(
            @RequestParam(required = false) Integer limit) {
        return dashboardService.getRecentMovements(limit);
    }

    @GetMapping("/entries/recent")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public List<DashboardRecentEntryResponse> recentEntries(
            @RequestParam(required = false) Integer limit) {
        return dashboardService.getRecentEntries(limit);
    }

    @GetMapping("/exits/recent")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public List<DashboardRecentExitResponse> recentExits(
            @RequestParam(required = false) Integer limit) {
        return dashboardService.getRecentExits(limit);
    }

    @GetMapping("/products/top-moved")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public List<TopMovedProductResponse> topMovedProducts(
            @RequestParam(required = false) Integer limit) {
        return dashboardService.getTopMovedProducts(limit);
    }

    @GetMapping("/warehouses")
    @PreAuthorize("@permissionChecker.has(authentication, 'dashboard.read')")
    public List<WarehouseStockSummaryResponse> warehouses() {
        return dashboardService.getWarehouseSummary();
    }
}
