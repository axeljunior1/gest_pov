package com.erp.products.controller;

import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.dto.AlertResponse;
import com.erp.products.mapper.AlertMapper;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertMapper alertMapper;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        var alerts = status == AlertStatus.OPEN
                ? alertService.findOpen()
                : status != null
                    ? alertService.findAll().stream()
                        .filter(a -> a.getStatus() == status)
                        .toList()
                    : alertService.findAll();
        return alerts.stream().map(alertMapper::toAlertResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.read')")
    public AlertResponse getById(@PathVariable Long id) {
        return alertMapper.toAlertResponse(alertService.getById(id));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public AlertResponse acknowledge(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.acknowledge(id, user));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public AlertResponse resolve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.resolve(id, user));
    }

    @PostMapping("/{id}/ignore")
    @PreAuthorize("@permissionChecker.has(authentication, 'stock.adjust')")
    public AlertResponse ignore(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.ignore(id, user));
    }

    private String resolveActor(String fallback) {
        if (currentUserService.isAuthenticated()) {
            return currentUserService.getCurrentUserEmailOrDefault();
        }
        return fallback != null && !fallback.isBlank() ? fallback : "system";
    }
}
