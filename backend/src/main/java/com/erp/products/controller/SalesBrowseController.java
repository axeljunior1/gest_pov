package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.SalesBrowseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesBrowseController {

    private final SalesBrowseService browseService;

    @GetMapping("/browse")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.sale.read', 'pos.sale.read_own', 'analytics.sales.read', 'pos.report.read')")
    public BrowsePageResponse<SaleSummaryResponse> browseSales(SaleBrowseFilterRequest request) {
        return browseService.listSales(request);
    }

    @GetMapping("/browse/export")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'export.read', 'analytics.export', 'pos.report.read', 'analytics.sales.read')")
    public ResponseEntity<byte[]> exportSales(SaleBrowseFilterRequest request) {
        return csvResponse("ventes", browseService.exportSalesCsv(request));
    }

    @GetMapping("/returns/browse")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.return.read', 'analytics.sales.read', 'pos.report.read')")
    public BrowsePageResponse<SaleRefundSummaryResponse> browseReturns(SaleRefundBrowseFilterRequest request) {
        return browseService.listReturns(request);
    }

    @GetMapping("/returns/browse/export")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'export.read', 'analytics.export', 'pos.return.read', 'pos.report.read')")
    public ResponseEntity<byte[]> exportReturns(SaleRefundBrowseFilterRequest request) {
        return csvResponse("retours", browseService.exportReturnsCsv(request));
    }

    @GetMapping("/returns/{id}")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.return.read', 'analytics.sales.read', 'pos.report.read')")
    public SaleRefundResponse returnDetail(@PathVariable Long id) {
        return browseService.getReturnDetail(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.sale.read', 'pos.sale.read_own', 'analytics.sales.read', 'pos.report.read')")
    public SaleDetailResponse saleDetail(@PathVariable Long id) {
        return browseService.getSaleDetail(id);
    }

    private ResponseEntity<byte[]> csvResponse(String baseName, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + baseName + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
