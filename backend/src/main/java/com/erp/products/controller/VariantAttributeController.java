package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.VariantAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/variant-attributes")
@RequiredArgsConstructor
public class VariantAttributeController {

    private final VariantAttributeService variantAttributeService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    public List<VariantAttributeResponse> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return variantAttributeService.findAll(activeOnly);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    public VariantAttributeResponse get(@PathVariable Long id) {
        return variantAttributeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    public VariantAttributeResponse create(@Valid @RequestBody VariantAttributeRequest request) {
        return variantAttributeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    public VariantAttributeResponse update(@PathVariable Long id, @RequestBody VariantAttributeRequest request) {
        return variantAttributeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    public void delete(@PathVariable Long id) {
        variantAttributeService.delete(id);
    }

    @PostMapping("/{id}/values")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    public VariantAttributeValueResponse addValue(
            @PathVariable Long id,
            @RequestBody VariantAttributeValueRequest request) {
        return variantAttributeService.addValue(id, request);
    }
}
