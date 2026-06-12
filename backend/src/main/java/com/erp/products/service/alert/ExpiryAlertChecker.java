package com.erp.products.service.alert;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ExpiryAlertChecker {

    private final LotRepository lotRepository;
    private final AlertSettingResolver settingResolver;
    private final AlertService alertService;

    public void checkAll() {
        LocalDate today = LocalDate.now();
        lotRepository.findAll().forEach(lot -> {
            if (lot.getDatePeremption() == null) {
                return;
            }
            var settings = settingResolver.resolve(lot.getProduct().getId(), null);
            int alertDays = settings.expiryAlertDays() != null ? settings.expiryAlertDays() : 30;

            AlertService.AlertPosition position = new AlertService.AlertPosition(
                    lot.getProduct(), null, null, lot);

            if (lot.getDatePeremption().isBefore(today)) {
                alertService.triggerIfNeeded(
                        AlertType.EXPIRED,
                        AlertSeverity.CRITICAL,
                        position,
                        BigDecimal.ZERO,
                        null,
                        "Lot " + lot.getNumeroLot() + " expire le " + lot.getDatePeremption());
                alertService.autoResolveIfOpen(AlertType.EXPIRY_SOON, position);
            } else if (!lot.getDatePeremption().isAfter(today.plusDays(alertDays))) {
                alertService.triggerIfNeeded(
                        AlertType.EXPIRY_SOON,
                        AlertSeverity.WARNING,
                        position,
                        BigDecimal.valueOf(alertDays),
                        null,
                        "Lot " + lot.getNumeroLot() + " perime le " + lot.getDatePeremption());
                alertService.autoResolveIfOpen(AlertType.EXPIRED, position);
            } else {
                alertService.autoResolveIfOpen(AlertType.EXPIRED, position);
                alertService.autoResolveIfOpen(AlertType.EXPIRY_SOON, position);
            }
        });
    }
}
