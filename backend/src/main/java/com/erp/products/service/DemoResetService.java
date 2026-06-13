package com.erp.products.service;

import com.erp.products.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoResetService {

    private final DataSource dataSource;

    @Transactional
    public void resetBusinessData() {
        log.warn("Purge des donnees metier (referentiel systeme conserve)");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/reset-demo.sql"));
        populator.setContinueOnError(false);
        try {
            populator.execute(dataSource);
        } catch (Exception e) {
            throw new BusinessException("Echec purge donnees metier: " + e.getMessage());
        }
        log.info("Donnees metier purgees");
    }
}
