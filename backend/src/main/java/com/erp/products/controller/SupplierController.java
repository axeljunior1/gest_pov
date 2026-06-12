package com.erp.products.controller;

import com.erp.products.dto.SupplierRequest;
import com.erp.products.dto.SupplierResponse;
import com.erp.products.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping
    public List<SupplierResponse> findAll() {
        return supplierService.findAll();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/search")
    public List<SupplierResponse> search(@RequestParam String nom) {
        return supplierService.search(nom);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}")
    public SupplierResponse getById(@PathVariable Long id) {
        return supplierService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
