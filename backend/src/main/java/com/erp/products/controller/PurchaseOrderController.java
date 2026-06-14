package com.erp.products.controller;

import com.erp.products.domain.enums.PurchaseOrderStatus;
import com.erp.products.dto.PurchaseOrderReceiveRequest;
import com.erp.products.dto.PurchaseOrderRequest;
import com.erp.products.dto.PurchaseOrderResponse;
import com.erp.products.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.read')")
    @GetMapping
    public List<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId) {
        return purchaseOrderService.list(status, supplierId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.read')")
    @GetMapping("/{id}")
    public PurchaseOrderResponse getById(@PathVariable Long id) {
        return purchaseOrderService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.update')")
    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancel(@PathVariable Long id) {
        return purchaseOrderService.cancel(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.validate')")
    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receive(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderReceiveRequest request) {
        return purchaseOrderService.receive(id, request);
    }
}
