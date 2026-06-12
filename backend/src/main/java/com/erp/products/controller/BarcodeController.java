package com.erp.products.controller;

import com.erp.products.dto.BarcodeGenerateRequest;
import com.erp.products.service.BarcodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/barcodes")
@RequiredArgsConstructor
public class BarcodeController {

    private final BarcodeService barcodeService;

    @PostMapping("/generate")
    @PreAuthorize("@permissionChecker.has(authentication, 'products.update')")
    public Map<String, String> generate(@Valid @RequestBody BarcodeGenerateRequest request) {
        String image = barcodeService.generateBase64(request.getContent(), request.getType());
        return Map.of("imageBase64", image, "content", request.getContent(), "type", request.getType().name());
    }
}
