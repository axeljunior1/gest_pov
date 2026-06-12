package com.erp.products.controller;

import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
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
    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.read')")
    public List<AlertResponse> list(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId) {
        return alertService.findFiltered(type, status, productId, warehouseId).stream()
                .map(alertMapper::toAlertResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.read')")
    public AlertResponse getById(@PathVariable Long id) {
        return alertMapper.toAlertResponse(alertService.getById(id));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    public AlertResponse acknowledge(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = currentUserService.resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.acknowledge(id, user));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    public AlertResponse resolve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = currentUserService.resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.resolve(id, user));
    }

    @PostMapping("/{id}/ignore")
    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    public AlertResponse ignore(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String user = currentUserService.resolveActor(body != null ? body.get("user") : null);
        return alertMapper.toAlertResponse(alertService.ignore(id, user));
    }
}

