package com.erp.products.config;

import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AppSettingsInitializer implements ApplicationRunner {

    private final SettingsService settingsService;
    private final SeedProperties seedProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedProperties.isSystemEnabled()) {
            log.info("Seed systeme desactive (app.seed.system-enabled=false)");
            return;
        }
        settingsService.ensureDefaultsSeeded();
        log.info("Parametres applicatifs initialises");
    }
}
