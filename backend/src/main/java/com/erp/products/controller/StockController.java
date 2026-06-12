package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockReservationService reservationService;
    private final StockTransferService transferService;

    @GetMapping("/items")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public List<StockItemResponse> listItems(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {
        return stockService.listItems(warehouseId, productId);
    }

    @GetMapping("/available")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public StockAvailableResponse getAvailable(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Long warehouseId) {
        return stockService.getAvailable(productId, variantId, warehouseId);
    }

    @PostMapping("/receipt")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockMovementResponse receive(@Valid @RequestBody StockOperationRequest request) {
        return stockService.receive(request);
    }

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockMovementResponse issue(@Valid @RequestBody StockOperationRequest request) {
        return stockService.issue(request);
    }

    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockMovementResponse adjust(@Valid @RequestBody StockOperationRequest request) {
        return stockService.adjust(request);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockReservationResponse reserve(@Valid @RequestBody StockReservationRequest request) {
        return reservationService.reserve(request);
    }

    @GetMapping("/reservations")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public List<StockReservationResponse> activeReservations() {
        return reservationService.findActive();
    }

    @PostMapping("/reservations/{id}/release")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockReservationResponse releaseReservation(@PathVariable Long id) {
        return reservationService.release(id);
    }

    @PostMapping("/reservations/{id}/consume")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockReservationResponse consumeReservation(@PathVariable Long id) {
        return reservationService.consume(id);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockTransferResponse createTransfer(@Valid @RequestBody StockTransferRequest request) {
        return transferService.create(request);
    }

    @GetMapping("/transfers")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public List<StockTransferResponse> listTransfers() {
        return transferService.findAll();
    }

    @GetMapping("/transfers/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public StockTransferResponse getTransfer(@PathVariable Long id) {
        return transferService.getById(id);
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockTransferResponse shipTransfer(@PathVariable Long id) {
        return transferService.ship(id);
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public StockTransferResponse receiveTransfer(@PathVariable Long id) {
        return transferService.receive(id);
    }
}
