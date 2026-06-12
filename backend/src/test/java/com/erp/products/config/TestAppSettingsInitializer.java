package com.erp.products.config;

import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestAppSettingsInitializer implements ApplicationRunner {

    private final SettingsService settingsService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        settingsService.ensureDefaultsSeeded();
    }
}
