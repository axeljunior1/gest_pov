package com.erp.products.controller;

import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import com.erp.products.dto.StockExitRequest;
import com.erp.products.dto.StockExitResponse;
import com.erp.products.service.StockExitService;
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
@RequestMapping("/api/stock/exits")
@RequiredArgsConstructor
public class StockExitController {

    private final StockExitService stockExitService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.read')")
    public List<StockExitResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockExitStatus status,
            @RequestParam(required = false) StockExitReason reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return stockExitService.findAll(productId, warehouseId, status, reason, dateFrom, dateTo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.read')")
    public StockExitResponse getById(@PathVariable Long id) {
        return stockExitService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.create')")
    public StockExitResponse create(@Valid @RequestBody StockExitRequest request) {
        return stockExitService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.update')")
    public StockExitResponse update(@PathVariable Long id, @Valid @RequestBody StockExitRequest request) {
        return stockExitService.update(id, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.validate')")
    public StockExitResponse validate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return stockExitService.validate(id, user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.cancel')")
    public StockExitResponse cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return stockExitService.cancel(id, user);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.update')")
    public void deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        stockExitService.deleteLine(id, lineId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock_exit.update')")
    public void delete(@PathVariable Long id) {
        stockExitService.delete(id);
    }
}
