package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping
    public List<UnitOfMeasureResponse> findAll() {
        return unitService.findAll();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitOfMeasureResponse create(@Valid @RequestBody UnitOfMeasureRequest request) {
        return unitService.create(request);
    }

    /** Conversions universelles (kg↔g, L↔mL…) — indépendantes des produits */
    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping("/conversions")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitConversionResponse createConversion(@Valid @RequestBody UnitConversionRequest request) {
        return unitService.createConversion(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/conversions")
    public List<UnitConversionResponse> getGlobalConversions() {
        return unitService.getGlobalConversions();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/convert")
    public Map<String, BigDecimal> convertGlobal(
            @RequestParam Long fromUnitId,
            @RequestParam Long toUnitId,
            @RequestParam BigDecimal quantity) {
        BigDecimal result = unitService.convertGlobal(fromUnitId, toUnitId, quantity);
        return Map.of("result", result);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        unitService.delete(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/conversions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversion(@PathVariable Long id) {
        unitService.deleteConversion(id);
    }
}
