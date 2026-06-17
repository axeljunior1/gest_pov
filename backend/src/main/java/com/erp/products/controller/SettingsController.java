package com.erp.products.controller;

import com.erp.products.dto.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.ReferenceValueService;
import com.erp.products.service.SettingsService;
import com.erp.products.service.ClientConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final ClientConfigurationService clientConfigurationService;
    private final ReferenceValueService referenceValueService;
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

    @GetMapping("/reference-values")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public Map<String, List<ReferenceValueResponse>> referenceValues() {
        return referenceValueService.listAllGrouped();
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

    @GetMapping("/client-config")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public ClientConfigurationResponse clientConfig() {
        return clientConfigurationService.getClientConfiguration();
    }

    @PutMapping("/client-config")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public ClientConfigurationResponse updateClientConfig(@Valid @RequestBody ClientConfigurationUpdateRequest request) {
        return clientConfigurationService.updateClientConfiguration(
                request, currentUserService.getCurrentUserEmailOrDefault());
    }

    @PostMapping(value = "/company/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public ClientConfigurationResponse uploadCompanyLogo(@RequestParam("file") MultipartFile file) {
        return clientConfigurationService.uploadCompanyLogo(file, currentUserService.getCurrentUserEmailOrDefault());
    }
}
