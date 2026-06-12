package com.erp.products.controller;

import com.erp.products.domain.enums.*;
import com.erp.products.dto.ProductSearchCriteria;
import com.erp.products.dto.StockMovementSearchCriteria;
import com.erp.products.service.ExportService;
import com.erp.products.util.TabularFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/products")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) String query) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setQuery(query);
        return fileResponse("products", format, exportService.exportProducts(format, criteria));
    }

    @GetMapping("/stock")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportStock(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {
        return fileResponse("stock", format, exportService.exportStock(format, warehouseId, productId));
    }

    @GetMapping("/movements")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        StockMovementSearchCriteria criteria = new StockMovementSearchCriteria();
        criteria.setProductId(productId);
        criteria.setWarehouseId(warehouseId);
        criteria.setMovementType(type);
        if (dateFrom != null) {
            criteria.setDateFrom(dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        if (dateTo != null) {
            criteria.setDateTo(dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1));
        }
        criteria.setLimit(5000);
        return fileResponse("stock-movements", format, exportService.exportMovements(format, criteria));
    }

    @GetMapping("/entries")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportEntries(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockEntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return fileResponse("stock-entries", format,
                exportService.exportEntries(format, productId, warehouseId, status, dateFrom, dateTo));
    }

    @GetMapping("/exits")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportExits(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockExitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return fileResponse("stock-exits", format,
                exportService.exportExits(format, productId, warehouseId, status, dateFrom, dateTo));
    }

    @GetMapping("/alerts")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportAlerts(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId) {
        return fileResponse("alerts", format,
                exportService.exportAlerts(format, type, status, productId, warehouseId));
    }

    @GetMapping("/inventories")
    @PreAuthorize("@permissionChecker.has(authentication, 'export.read')")
    public ResponseEntity<byte[]> exportInventories(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) InventoryCountStatus status) {
        return fileResponse("inventories", format,
                exportService.exportInventories(format, warehouseId, status));
    }

    private ResponseEntity<byte[]> fileResponse(String baseName, ExportFormat format, byte[] body) {
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
