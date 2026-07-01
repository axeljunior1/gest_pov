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
 * Synchronise les listes de valeurs applicatives (devise, langue, valorisation stock, etc.)
 * avec le catalogue de reference au demarrage.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ReferenceValueDataInitializer implements ApplicationRunner {

    private final AppReferenceValueRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        for (ReferenceValueCatalog.Seed seed : ReferenceValueCatalog.all()) {
            if (repository.findByCategoryAndCodeIgnoreCase(seed.category(), seed.code()).isEmpty()) {
                repository.save(AppReferenceValue.builder()
                        .category(seed.category())
                        .code(seed.code())
                        .label(seed.label())
                        .sortOrder(seed.sortOrder())
                        .active(true)
                        .build());
                added++;
            }
        }
        if (added > 0) {
            log.info("Listes de valeurs applicatives : {} entree(s) ajoutee(s)", added);
        }
    }
}
