package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.CustomerService;
import com.erp.products.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public List<CustomerResponse> list() {
        return customerService.listAll();
    }

    @GetMapping("/search")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public List<CustomerResponse> search(@RequestParam String q) {
        return customerService.search(q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public CustomerHistoryResponse history(@PathVariable Long id) {
        return customerService.getHistory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.create')")
    public CustomerResponse create(@RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.update')")
    public CustomerResponse update(@PathVariable Long id, @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.delete')")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    @GetMapping("/{id}/loyalty/transactions")
    @PreAuthorize("@permissionChecker.has(authentication, 'loyalty.read')")
    public List<LoyaltyTransactionResponse> transactions(@PathVariable Long id) {
        return loyaltyService.listTransactions(id);
    }

    @PostMapping("/{id}/loyalty/adjust")
    @PreAuthorize("@permissionChecker.has(authentication, 'loyalty.manage')")
    public CustomerResponse adjustPoints(@PathVariable Long id, @RequestBody LoyaltyAdjustRequest request) {
        boolean add = request.getPoints() != null && request.getPoints() > 0;
        int points = request.getPoints() != null ? Math.abs(request.getPoints()) : 0;
        if (request.getPoints() != null && request.getPoints() < 0) {
            loyaltyService.manualAdjust(id, points, request.getReason(), false);
        } else {
            loyaltyService.manualAdjust(id, points, request.getReason(), add);
        }
        return customerService.getById(id);
    }
}
