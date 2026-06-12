package com.erp.products.controller;

import com.erp.products.domain.enums.DuplicateSkuMode;
import com.erp.products.domain.enums.ExportFormat;
import com.erp.products.dto.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.ImportService;
import com.erp.products.util.TabularFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final CurrentUserService currentUserService;

    @GetMapping("/templates/products")
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ResponseEntity<byte[]> productTemplate(@RequestParam(defaultValue = "CSV") ExportFormat format) {
        return templateResponse("template-produits", format, importService.productTemplate(format));
    }

    @GetMapping("/templates/packagings")
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ResponseEntity<byte[]> packagingTemplate(@RequestParam(defaultValue = "CSV") ExportFormat format) {
        return templateResponse("template-conditionnements", format, importService.packagingTemplate(format));
    }

    @GetMapping("/templates/initial-stock")
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ResponseEntity<byte[]> initialStockTemplate(@RequestParam(defaultValue = "CSV") ExportFormat format) {
        return templateResponse("template-stock-initial", format, importService.initialStockTemplate(format));
    }

    @PostMapping(value = "/products/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ImportPreviewResponse previewProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "REJECT") DuplicateSkuMode duplicateMode) {
        return importService.previewProducts(file, duplicateMode);
    }

    @PostMapping(value = "/products/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.create')")
    public ImportValidateResponse validateProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "REJECT") DuplicateSkuMode duplicateMode) {
        return importService.validateProducts(file, duplicateMode, currentUserService.getCurrentUserEmail());
    }

    @PostMapping(value = "/packagings/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ImportPreviewResponse previewPackagings(@RequestPart("file") MultipartFile file) {
        return importService.previewPackagings(file);
    }

    @PostMapping(value = "/packagings/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.create')")
    public ImportValidateResponse validatePackagings(@RequestPart("file") MultipartFile file) {
        return importService.validatePackagings(file, currentUserService.getCurrentUserEmail());
    }

    @PostMapping(value = "/initial-stock/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ImportPreviewResponse previewInitialStock(@RequestPart("file") MultipartFile file) {
        return importService.previewInitialStock(file);
    }

    @PostMapping(value = "/initial-stock/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'import.create')")
    public ImportValidateResponse validateInitialStock(@RequestPart("file") MultipartFile file) {
        return importService.validateInitialStock(file, currentUserService.getCurrentUserEmail());
    }

    @GetMapping("/history")
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public List<ImportJobResponse> history() {
        return importService.listHistory();
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'import.read')")
    public ImportJobResponse historyDetail(@PathVariable Long id) {
        return importService.getHistory(id);
    }

    private ResponseEntity<byte[]> templateResponse(String baseName, ExportFormat format, byte[] body) {
        String ext = TabularFileHelper.extension(format);
        MediaType mediaType = format == ExportFormat.XLSX
                ? MediaType.parseMediaType(TabularFileHelper.contentType(format))
                : new MediaType("text", "csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + baseName + "." + ext + "\"")
                .contentType(mediaType)
                .body(body);
    }
}
