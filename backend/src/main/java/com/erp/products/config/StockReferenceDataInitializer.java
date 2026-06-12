package com.erp.products.config;

import com.erp.products.domain.entity.Location;
import com.erp.products.domain.entity.Warehouse;
import com.erp.products.repository.LocationRepository;
import com.erp.products.repository.WarehouseRepository;
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
public class StockReferenceDataInitializer implements ApplicationRunner {

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (warehouseRepository.count() > 0) {
            return;
        }
        log.info("Initialisation entrepot par defaut...");
        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MAIN")
                .nom("Entrepot principal")
                .adresse("Site principal")
                .actif(true)
                .build());
        locationRepository.save(Location.builder()
                .warehouse(warehouse)
                .code("DEFAULT")
                .nom("Zone par defaut")
                .actif(true)
                .build());
        log.info("Entrepot {} et emplacement DEFAULT crees", warehouse.getCode());
    }
}
