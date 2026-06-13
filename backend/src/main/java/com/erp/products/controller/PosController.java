package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosSessionService sessionService;
    private final PosSaleService saleService;
    private final PosCatalogService catalogService;
    private final PosTicketService ticketService;
    private final PosRefundService refundService;
    private final SettingsService settingsService;
    private final PosConfigService posConfigService;
    private final CustomerService customerService;

    @GetMapping("/context")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public Map<String, Object> context() {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("session", sessionService.getCurrentSessionOrNull());
        body.put("posConfig", posConfigService.getConfig());
        body.put("registerName", settingsService.getSetting(com.erp.products.settings.SettingKeys.POS_REGISTER_NAME));
        body.put("publicSettings", settingsService.getPublicSettings());
        body.put("loyaltyConfig", settingsService.getLoyaltyConfig());
        return body;
    }

    @PostMapping("/sessions/open")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.session.open', 'pos.sale.send_to_payment', 'pos.sale.prepare')")
    public PosSessionResponse openSession(@RequestBody PosSessionOpenRequest request) {
        return sessionService.openSession(request);
    }

    @GetMapping("/sessions/current")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public PosSessionResponse currentSession() {
        return sessionService.getCurrentSession();
    }

    @PostMapping("/sessions/close")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.session.close', 'pos.payment.collect', 'pos.sale.send_to_payment', 'pos.sale.prepare')")
    public PosSessionReportResponse closeSession(@RequestBody PosSessionCloseRequest request) {
        return sessionService.closeSession(request);
    }

    @GetMapping("/sessions/current/close-preview")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.session.close', 'pos.payment.collect')")
    public PosSessionReportResponse closePreview() {
        return sessionService.getClosePreview();
    }

    @GetMapping("/sessions/{id}/report")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.report.read')")
    public PosSessionReportResponse sessionReport(@PathVariable Long id) {
        return sessionService.getSessionReport(id);
    }

    @GetMapping("/catalog")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public PosCatalogResponse catalog(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId) {
        return catalogService.getCatalog(warehouseId, categoryId);
    }

    @GetMapping("/catalog/search")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public PosSearchResultResponse search(
            @RequestParam String q,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId) {
        return catalogService.search(q, warehouseId, categoryId);
    }

    @GetMapping("/catalog/products/{productId}")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public PosProductResponse getProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) Long warehouseId) {
        return catalogService.getProduct(productId, warehouseId);
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.create')")
    public SaleResponse createSale() {
        return saleService.createSale();
    }

    @GetMapping("/sales/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public SaleResponse getSale(@PathVariable Long id) {
        return saleService.getSale(id);
    }

    @PostMapping("/sales/{id}/lines")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.create')")
    public SaleResponse addLine(@PathVariable Long id, @RequestBody SaleLineRequest request) {
        return saleService.upsertLine(id, request);
    }

    @PutMapping("/sales/{id}/lines/{lineId}")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.create')")
    public SaleResponse updateLine(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody Map<String, BigDecimal> body) {
        return saleService.updateLineQuantity(id, lineId, body.get("quantity"));
    }

    @PutMapping("/sales/{id}/lines/{lineId}/discount")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.discount')")
    public SaleResponse lineDiscount(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody Map<String, BigDecimal> body) {
        return saleService.applyLineDiscount(id, lineId, body.get("discountAmount"));
    }

    @PostMapping("/sales/{id}/hold")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.create')")
    public SaleResponse holdSale(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String label = body != null ? body.get("label") : null;
        return saleService.holdSale(id, label);
    }

    @PostMapping("/sales/{id}/resume")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.create')")
    public SaleResponse resumeSale(@PathVariable Long id) {
        return saleService.resumeSale(id);
    }

    @DeleteMapping("/sales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.cancel')")
    public void deleteHold(@PathVariable Long id) {
        saleService.deleteHoldSale(id);
    }

    @GetMapping("/sales/hold")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.read')")
    public List<SaleResponse> listHold() {
        return saleService.listHoldSales();
    }

    @GetMapping("/sales/pending-payment")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.payment.collect')")
    public List<SaleResponse> listPendingPayments() {
        return saleService.listPendingPayments();
    }

    @PostMapping("/sales/{id}/send-to-payment")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.sale.send_to_payment', 'pos.sale.prepare')")
    public SaleResponse sendToPayment(@PathVariable Long id) {
        return saleService.sendToPayment(id);
    }

    @PostMapping("/sales/{id}/submit-payment")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.sale.send_to_payment', 'pos.sale.prepare')")
    public SaleResponse submitForPayment(@PathVariable Long id) {
        return saleService.submitForPayment(id);
    }

    @PostMapping("/sales/{id}/validate")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.payment.collect', 'pos.payment.validate', 'pos.sale.validate')")
    public SaleResponse validateSale(@PathVariable Long id, @RequestBody SaleValidateRequest request) {
        return saleService.validateSale(id, request);
    }

    @PostMapping("/sales/{id}/cancel")
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.cancel')")
    public SaleResponse cancelSale(@PathVariable Long id) {
        return saleService.cancelSale(id);
    }

    @GetMapping("/sales/{id}/ticket")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'pos.ticket.print', 'pos.ticket.reprint')")
    public TicketResponse ticket(@PathVariable Long id) {
        return ticketService.buildTicket(id);
    }

    @PostMapping("/sales/{id}/refund")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'pos.sale.refund')")
    public SaleRefundResponse refund(@PathVariable Long id, @RequestBody SaleRefundRequest request) {
        return refundService.createRefund(id, request);
    }

    @GetMapping("/customers/search")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public List<CustomerResponse> searchCustomers(@RequestParam String q) {
        return customerService.search(q);
    }

    @PostMapping("/customers/quick")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.create')")
    public CustomerResponse quickCreateCustomer(@RequestBody CustomerQuickCreateRequest request) {
        return customerService.quickCreate(request);
    }

    @PutMapping("/sales/{id}/customer")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public SaleResponse assignCustomer(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        return saleService.assignCustomer(id, body.get("customerId"));
    }

    @DeleteMapping("/sales/{id}/customer")
    @PreAuthorize("@permissionChecker.has(authentication, 'customer.read')")
    public SaleResponse removeCustomer(@PathVariable Long id) {
        return saleService.removeCustomer(id);
    }

    @PostMapping("/sales/{id}/loyalty/redeem")
    @PreAuthorize("@permissionChecker.has(authentication, 'loyalty.redeem')")
    public SaleResponse redeemLoyalty(@PathVariable Long id, @RequestBody LoyaltyRedeemRequest request) {
        return saleService.applyLoyaltyRedemption(id, request);
    }

    @DeleteMapping("/sales/{id}/loyalty/redeem")
    @PreAuthorize("@permissionChecker.has(authentication, 'loyalty.redeem')")
    public SaleResponse clearLoyaltyRedemption(@PathVariable Long id) {
        return saleService.clearLoyaltyRedemption(id);
    }
}
