package com.erp.products.config;

import com.erp.products.domain.entity.AppReferenceValue;
import com.erp.products.reference.ReferenceValueCatalog;
import com.erp.products.repository.AppReferenceValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed des listes de valeurs en profil test (Flyway desactive, schema Hibernate).
 */
@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class ReferenceValueTestInitializer implements ApplicationRunner {

    private final AppReferenceValueRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        log.info("Initialisation des listes de valeurs applicatives (test)...");
        ReferenceValueCatalog.all().forEach(seed -> repository.save(AppReferenceValue.builder()
                .category(seed.category())
                .code(seed.code())
                .label(seed.label())
                .sortOrder(seed.sortOrder())
                .active(true)
                .build()));
    }
}
