package com.erp.products.controller;

import com.erp.products.dto.CustomAttributeDefinitionRequest;
import com.erp.products.dto.CustomAttributeDefinitionResponse;
import com.erp.products.service.CustomAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class CustomAttributeController {

    private final CustomAttributeService customAttributeService;

    @GetMapping
    public List<CustomAttributeDefinitionResponse> findAll() {
        return customAttributeService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomAttributeDefinitionResponse create(@Valid @RequestBody CustomAttributeDefinitionRequest request) {
        return customAttributeService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customAttributeService.delete(id);
    }
}
