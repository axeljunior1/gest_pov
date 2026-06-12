package com.erp.products.service.alert;

import com.erp.products.domain.entity.Lot;
import com.erp.products.domain.entity.Location;
import com.erp.products.domain.entity.Warehouse;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.LotRepository;
import com.erp.products.repository.LocationRepository;
import com.erp.products.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ExpiryAlertChecker {

    private final LotRepository lotRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final AlertSettingResolver settingResolver;
    private final AlertService alertService;

    public void checkAll() {
        lotRepository.findAll().forEach(lot -> checkLot(lot.getId(), null, null));
    }

    public void checkLot(Long lotId, Long warehouseId, Long locationId) {
        if (lotId == null) {
            return;
        }
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot non trouvé: " + lotId));
        Warehouse warehouse = warehouseId != null
                ? warehouseRepository.findById(warehouseId).orElse(null)
                : null;
        Location location = locationId != null && warehouseId != null
                ? locationRepository.findByIdAndWarehouseId(locationId, warehouseId).orElse(null)
                : null;
        evaluateLot(lot, warehouse, location);
    }

    private void evaluateLot(Lot lot, Warehouse warehouse, Location location) {
        if (lot.getDatePeremption() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        var settings = settingResolver.resolve(lot.getProduct().getId(),
                warehouse != null ? warehouse.getId() : null);
        int alertDays = settings.expiryAlertDays() != null ? settings.expiryAlertDays() : 30;

        AlertService.AlertPosition position = new AlertService.AlertPosition(
                lot.getProduct(), warehouse, location, lot);

        if (lot.getDatePeremption().isBefore(today)) {
            alertService.triggerIfNeeded(
                    AlertType.EXPIRED,
                    AlertSeverity.CRITICAL,
                    position,
                    BigDecimal.ZERO,
                    null,
                    "Lot " + lot.getNumeroLot() + " expiré le " + lot.getDatePeremption());
            alertService.autoResolveIfOpen(AlertType.EXPIRY_SOON, position);
        } else if (!lot.getDatePeremption().isAfter(today.plusDays(alertDays))) {
            alertService.triggerIfNeeded(
                    AlertType.EXPIRY_SOON,
                    AlertSeverity.WARNING,
                    position,
                    BigDecimal.valueOf(alertDays),
                    null,
                    "Lot " + lot.getNumeroLot() + " expire le " + lot.getDatePeremption());
            alertService.autoResolveIfOpen(AlertType.EXPIRED, position);
        } else {
            alertService.autoResolveIfOpen(AlertType.EXPIRED, position);
            alertService.autoResolveIfOpen(AlertType.EXPIRY_SOON, position);
        }
    }
}
