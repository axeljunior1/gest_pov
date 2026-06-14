package com.erp.products.controller;

import com.erp.products.dto.CancelSaleRequest;
import com.erp.products.dto.CancelledSaleDetailResponse;
import com.erp.products.dto.CancelledSaleFilterRequest;
import com.erp.products.dto.CancelledSaleSummaryResponse;
import com.erp.products.dto.analytics.AnalyticsFilterRequest;
import com.erp.products.dto.analytics.CancellationAnalyticsResponse;
import com.erp.products.service.SaleCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/cancellations")
@RequiredArgsConstructor
public class SaleCancellationController {

    private final SaleCancellationService cancellationService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'sales.cancellations.read', 'analytics.read')")
    public List<CancelledSaleSummaryResponse> list(CancelledSaleFilterRequest request) {
        return cancellationService.list(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'sales.cancellations.read', 'sales.cancellations.detail', 'sales.cancellations.audit')")
    public CancelledSaleDetailResponse detail(@PathVariable Long id) {
        return cancellationService.getDetail(id);
    }

    @GetMapping("/analytics")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'sales.cancellations.read', 'analytics.read')")
    public CancellationAnalyticsResponse analytics(AnalyticsFilterRequest request) {
        return cancellationService.getAnalytics(request);
    }

    @GetMapping("/reasons")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.sale.cancel', 'sales.cancellations.read')")
    public List<SaleCancellationService.SaleCancellationReasonOption> reasons() {
        return cancellationService.listReasons();
    }
}
