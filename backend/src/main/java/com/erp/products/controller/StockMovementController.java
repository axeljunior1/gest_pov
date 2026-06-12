package com.erp.products.controller;

import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockMovementResponse;
import com.erp.products.dto.StockMovementSearchCriteria;
import com.erp.products.service.StockMovementService;
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
import java.util.List;

@RestController
@RequestMapping("/api/stock/movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService movementService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'stock_movement.read', 'stock.read')")
    public List<StockMovementResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer limit) {

        StockMovementSearchCriteria criteria = buildCriteria(
                productId, warehouseId, locationId, type, referenceType, referenceId,
                reference, createdBy, dateFrom, dateTo, limit);

        if (referenceType != null && referenceId != null && productId == null && warehouseId == null
                && locationId == null && type == null && reference == null && createdBy == null
                && dateFrom == null && dateTo == null) {
            return movementService.getMovementsByReference(referenceType, referenceId);
        }
        return movementService.listMovements(criteria);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'stock_movement.read', 'stock.read')")
    public StockMovementResponse getById(@PathVariable Long id) {
        return movementService.getMovementById(id);
    }

    @GetMapping("/export")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'stock_movement.export', 'stock_movement.read', 'stock.read')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        StockMovementSearchCriteria criteria = buildCriteria(
                productId, warehouseId, locationId, type, referenceType, referenceId,
                reference, createdBy, dateFrom, dateTo, 5000);

        byte[] csv = movementService.exportMovements(criteria);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock-movements.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    private StockMovementSearchCriteria buildCriteria(
            Long productId,
            Long warehouseId,
            Long locationId,
            StockMovementType type,
            String referenceType,
            Long referenceId,
            String reference,
            String createdBy,
            LocalDate dateFrom,
            LocalDate dateTo,
            Integer limit) {

        StockMovementSearchCriteria criteria = new StockMovementSearchCriteria();
        criteria.setProductId(productId);
        criteria.setWarehouseId(warehouseId);
        criteria.setLocationId(locationId);
        criteria.setMovementType(type);
        criteria.setReferenceType(referenceType);
        criteria.setReferenceId(referenceId);
        criteria.setReference(reference);
        criteria.setCreatedBy(createdBy);
        if (dateFrom != null) {
            criteria.setDateFrom(dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        if (dateTo != null) {
            criteria.setDateTo(dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1));
        }
        criteria.setLimit(limit);
        return criteria;
    }
}
