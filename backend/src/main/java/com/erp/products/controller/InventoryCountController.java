package com.erp.products.controller;

import com.erp.products.domain.enums.InventoryCountStatus;
import com.erp.products.dto.InventoryCountRequest;
import com.erp.products.dto.InventoryCountResponse;
import com.erp.products.service.InventoryService;
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
@RequestMapping("/api/stock/inventories")
@RequiredArgsConstructor
public class InventoryCountController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.read')")
    public List<InventoryCountResponse> list(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) InventoryCountStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return inventoryService.findAll(warehouseId, locationId, status, dateFrom, dateTo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.read')")
    public InventoryCountResponse getById(@PathVariable Long id) {
        return inventoryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.create')")
    public InventoryCountResponse create(@Valid @RequestBody InventoryCountRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.update')")
    public InventoryCountResponse update(@PathVariable Long id, @Valid @RequestBody InventoryCountRequest request) {
        return inventoryService.update(id, request);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.update')")
    public InventoryCountResponse start(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return inventoryService.start(id, user);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.validate')")
    public InventoryCountResponse validate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return inventoryService.validate(id, user);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.cancel')")
    public InventoryCountResponse cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = body != null ? body.get("user") : null;
        return inventoryService.cancel(id, user);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.update')")
    public void deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        inventoryService.deleteLine(id, lineId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'inventory.update')")
    public void delete(@PathVariable Long id) {
        inventoryService.delete(id);
    }
}
