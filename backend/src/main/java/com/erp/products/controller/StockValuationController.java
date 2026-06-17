package com.erp.products.controller;

import com.erp.products.dto.stockvaluation.*;
import com.erp.products.service.stockvaluation.StockCmpValuationService;
import com.erp.products.service.stockvaluation.StockValuationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock/valuation")
@RequiredArgsConstructor
public class StockValuationController {

    private final StockCmpValuationService cmpValuationService;
    private final StockValuationAnalyticsService analyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('stock.read')")
    public StockValuationOverviewResponse overview() {
        return analyticsService.getOverview();
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('stock.read')")
    public BigDecimal currentTotalValue() {
        return cmpValuationService.getTotalStockValue();
    }

    @GetMapping("/by-product")
    @PreAuthorize("hasAuthority('stock.read')")
    public List<StockValuationProductRow> byProduct() {
        return analyticsService.getStockValueByProduct();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('stock.read')")
    public List<StockValuationTrendPoint> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        return analyticsService.getStockValueHistory(from, to, granularity);
    }

    @GetMapping("/at-date")
    @PreAuthorize("hasAuthority('stock.read')")
    public BigDecimal atDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getStockValueAtDate(date);
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('stock.read')")
    public List<StockValuationProductRow> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTopProductsByValue(limit);
    }

    @GetMapping("/stale")
    @PreAuthorize("hasAuthority('stock.read')")
    public List<StockValuationStaleProductRow> staleProducts(
            @RequestParam(defaultValue = "90") int inactiveDays,
            @RequestParam(defaultValue = "20") int limit) {
        return analyticsService.getStaleProducts(inactiveDays, limit);
    }
}
