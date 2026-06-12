package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping
    public List<CategoryResponse> getTree() {
        return categoryService.getTree();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/search")
    public List<CategoryResponse> search(@RequestParam String nom) {
        return categoryService.search(nom);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.read')")
    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'products.delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
