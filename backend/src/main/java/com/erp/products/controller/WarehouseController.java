package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    @GetMapping
    public List<WarehouseResponse> findAll() {
        return warehouseService.findAll();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody WarehouseRequest request) {
        return warehouseService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    @GetMapping("/{warehouseId}/locations")
    public List<LocationResponse> listLocations(@PathVariable Long warehouseId) {
        return warehouseService.listLocations(warehouseId);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    @PostMapping("/{warehouseId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse addLocation(@PathVariable Long warehouseId,
                                        @Valid @RequestBody LocationRequest request) {
        return warehouseService.addLocation(warehouseId, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    @PostMapping("/lots")
    @ResponseStatus(HttpStatus.CREATED)
    public LotResponse createLot(@Valid @RequestBody LotRequest request) {
        return warehouseService.createLot(request);
    }
}
