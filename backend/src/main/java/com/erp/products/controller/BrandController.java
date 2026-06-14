package com.erp.products.controller;

import com.erp.products.dto.BrandRequest;
import com.erp.products.dto.BrandResponse;
import com.erp.products.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping
    public List<BrandResponse> findAll() {
        return brandService.findAll();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/search")
    public List<BrandResponse> search(@RequestParam String nom) {
        return brandService.search(nom);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}")
    public BrandResponse getById(@PathVariable Long id) {
        return brandService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse create(@Valid @RequestBody BrandRequest request) {
        return brandService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}")
    public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return brandService.update(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
    }
}
