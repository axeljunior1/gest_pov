package com.erp.products.controller;

import com.erp.products.config.AdminDevProperties;
import com.erp.products.exception.BusinessException;
import com.erp.products.service.DemoDataSeeder;
import com.erp.products.service.DemoResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Profile("dev")
@RequiredArgsConstructor
public class AdminDevController {

    private final AdminDevProperties adminDevProperties;
    private final DemoResetService demoResetService;
    private final DemoDataSeeder demoDataSeeder;

    @GetMapping("/dev-tools/status")
    @PreAuthorize("@permissionChecker.has(authentication, 'ROLE_SUPER_ADMIN')")
    public Map<String, Object> devToolsStatus() {
        return Map.of(
                "profile", "dev",
                "resetEnabled", adminDevProperties.isResetEnabled());
    }

    @PostMapping("/reset-demo")
    @PreAuthorize("@permissionChecker.has(authentication, 'ROLE_SUPER_ADMIN')")
    public Map<String, Object> resetDemo(@RequestHeader(value = "X-Reset-Token", required = false) String token) {
        assertDevResetAllowed(token);
        demoResetService.resetBusinessData();
        return Map.of(
                "status", "OK",
                "message", "Donnees metier purgees (referentiel systeme conserve)");
    }

    @PostMapping("/seed-demo")
    @PreAuthorize("@permissionChecker.has(authentication, 'ROLE_SUPER_ADMIN')")
    public Map<String, Object> seedDemo(@RequestHeader(value = "X-Reset-Token", required = false) String token) {
        assertDevResetAllowed(token);
        DemoDataSeeder.DemoSeedResult result = demoDataSeeder.seed();
        return Map.of(
                "status", result.status(),
                "productId", result.productId(),
                "productSku", result.productSku());
    }

    private void assertDevResetAllowed(String token) {
        if (!adminDevProperties.isResetEnabled()) {
            throw new BusinessException("Reset demo desactive (profil dev requis)");
        }
        String expected = adminDevProperties.getResetToken();
        if (expected == null || expected.isBlank() || !expected.equals(token)) {
            throw new BusinessException("Jeton X-Reset-Token invalide");
        }
    }
}
