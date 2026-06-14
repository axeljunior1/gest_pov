package com.erp.products.controller;

import com.erp.products.dto.AlertSettingRequest;
import com.erp.products.dto.AlertSettingResponse;
import com.erp.products.service.AlertSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-settings")
@RequiredArgsConstructor
public class AlertSettingController {

    private final AlertSettingService alertSettingService;

    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.read')")
    @GetMapping
    public List<AlertSettingResponse> list() {
        return alertSettingService.findAll();
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.read')")
    @GetMapping("/{id}")
    public AlertSettingResponse getById(@PathVariable Long id) {
        return alertSettingService.getById(id);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertSettingResponse create(@Valid @RequestBody AlertSettingRequest request) {
        return alertSettingService.create(request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    @PutMapping("/{id}")
    public AlertSettingResponse update(@PathVariable Long id, @Valid @RequestBody AlertSettingRequest request) {
        return alertSettingService.update(id, request);
    }

    @PreAuthorize("@permissionChecker.has(authentication, 'alerts.manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        alertSettingService.delete(id);
    }
}
