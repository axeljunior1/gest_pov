package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/public")
    public PublicSettingsResponse publicSettings() {
        return settingsService.getPublicSettings();
    }

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public List<AppSettingResponse> getAll() {
        return settingsService.getAllSettings();
    }

    @GetMapping("/{key:.+}")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public AppSettingResponse getByKey(@PathVariable String key) {
        return settingsService.getAllSettings().stream()
                .filter(s -> s.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new com.erp.products.exception.ResourceNotFoundException("Parametre: " + key));
    }

    @PutMapping("/{key:.+}")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public AppSettingResponse update(
            @PathVariable String key,
            @RequestBody SettingUpdateRequest request) {
        return settingsService.setSetting(key, request.getValue(),
                currentUserService.getCurrentUserEmailOrDefault());
    }

    @PutMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public List<AppSettingResponse> updateBulk(@RequestBody BulkSettingsUpdateRequest request) {
        return settingsService.updateSettings(request.getSettings(),
                currentUserService.getCurrentUserEmailOrDefault());
    }

    @GetMapping("/config/numbering")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public NumberingConfigResponse numberingConfig() {
        return settingsService.getNumberingConfig();
    }

    @GetMapping("/config/stock")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public StockConfigResponse stockConfig() {
        return settingsService.getStockConfig();
    }

    @GetMapping("/config/alerts")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public AlertConfigResponse alertConfig() {
        return settingsService.getAlertConfig();
    }

    @GetMapping("/config/loyalty")
    @PreAuthorize("@permissionChecker.hasAny(authentication, 'settings.read', 'loyalty.read', 'loyalty.settings.update')")
    public LoyaltyConfigResponse loyaltyConfig() {
        return settingsService.getLoyaltyConfig();
    }
}
