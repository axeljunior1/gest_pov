package com.erp.products.controller;

import com.erp.products.domain.enums.StockEntryStatus;
import com.erp.products.dto.StockEntryRequest;
import com.erp.products.dto.StockEntryResponse;
import com.erp.products.service.StockEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock/entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.read')")
    public List<StockEntryResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockEntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return stockEntryService.findAll(productId, supplierId, warehouseId, status, dateFrom, dateTo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.read')")
    public StockEntryResponse getById(@PathVariable Long id) {
        return stockEntryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.create')")
    public StockEntryResponse create(@Valid @RequestBody StockEntryRequest request) {
        return stockEntryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.update')")
    public StockEntryResponse update(@PathVariable Long id, @Valid @RequestBody StockEntryRequest request) {
        return stockEntryService.update(id, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.validate')")
    public StockEntryResponse validate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return stockEntryService.validate(id, user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.cancel')")
    public StockEntryResponse cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return stockEntryService.cancel(id, user);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.update')")
    public void deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        stockEntryService.deleteLine(id, lineId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_entry.update')")
    public void delete(@PathVariable Long id) {
        stockEntryService.delete(id);
    }
}
