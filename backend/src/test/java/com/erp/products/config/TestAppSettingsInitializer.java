package com.erp.products.config;

import com.erp.products.service.SettingsService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@Order(100)
@RequiredArgsConstructor
public class TestAppSettingsInitializer implements ApplicationRunner {

    private final SettingsService settingsService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAll();
    }

    /**
     * Reseed après nettoyage H2 (reference values déjà présentes via {@link ReferenceValueTestInitializer}).
     */
    @Transactional
    public void seedAll() {
        settingsService.ensureDefaultsSeeded();
        seedIntegrationTestDefaults();
    }

    /**
     * Valeurs attendues par les tests d'intégration (Settings, Analytics).
     * Ne modifie pas les defaults prod (company/currency vides jusqu'à configuration client).
     */
    private void seedIntegrationTestDefaults() {
        settingsService.setSetting(SettingKeys.COMPANY_NAME, "ERP Produits", "test-init");
        settingsService.setSetting(SettingKeys.APP_CURRENCY, "EUR", "test-init");
    }
}
