package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public List<UnitOfMeasureResponse> findAll() {
        return unitService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitOfMeasureResponse create(@Valid @RequestBody UnitOfMeasureRequest request) {
        return unitService.create(request);
    }

    /** Conversions universelles (kg↔g, L↔mL…) — indépendantes des produits */
    @PostMapping("/conversions")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitConversionResponse createConversion(@Valid @RequestBody UnitConversionRequest request) {
        return unitService.createConversion(request);
    }

    @GetMapping("/conversions")
    public List<UnitConversionResponse> getGlobalConversions() {
        return unitService.getGlobalConversions();
    }

    @GetMapping("/convert")
    public Map<String, BigDecimal> convertGlobal(
            @RequestParam Long fromUnitId,
            @RequestParam Long toUnitId,
            @RequestParam BigDecimal quantity) {
        BigDecimal result = unitService.convertGlobal(fromUnitId, toUnitId, quantity);
        return Map.of("result", result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        unitService.delete(id);
    }

    @DeleteMapping("/conversions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversion(@PathVariable Long id) {
        unitService.deleteConversion(id);
    }
}
