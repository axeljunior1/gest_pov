package com.erp.products.service.alert;

import com.erp.products.domain.entity.Alert;
import com.erp.products.domain.entity.Location;
import com.erp.products.domain.entity.StockEntry;
import com.erp.products.domain.entity.StockExit;
import com.erp.products.domain.entity.Warehouse;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.StockEntryStatus;
import com.erp.products.domain.enums.StockExitStatus;
import com.erp.products.repository.AlertRepository;
import com.erp.products.repository.StockEntryRepository;
import com.erp.products.repository.StockExitRepository;
import com.erp.products.service.SettingsService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Alerte quand une entree ou une sortie de stock reste en brouillon trop longtemps (paperasse
 * non finalisee — un utilisateur commence une saisie puis l'abandonne).
 */
@Component
@RequiredArgsConstructor
public class StaleDraftChecker {

    private final StockEntryRepository stockEntryRepository;
    private final StockExitRepository stockExitRepository;
    private final SettingsService settingsService;
    private final AlertService alertService;
    private final AlertRepository alertRepository;

    public void checkAll() {
        Integer hoursSetting = settingsService.getInteger(SettingKeys.STOCK_DRAFT_STALE_ALERT_HOURS);
        long hours = hoursSetting != null && hoursSetting > 0 ? hoursSetting : 24;
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        Set<String> stillStale = new HashSet<>();

        for (StockEntry entry : stockEntryRepository.findByStatusAndCreatedAtBefore(StockEntryStatus.DRAFT, cutoff)) {
            long ageHours = ChronoUnit.HOURS.between(entry.getCreatedAt(), Instant.now());
            trigger(entry.getWarehouse(), entry.getLocation(), hours, ageHours,
                    "Entree " + entry.getEntryNumber() + " en brouillon depuis " + ageHours
                            + "h (creee par " + entry.getCreatedBy() + ") — entrepot "
                            + entry.getWarehouse().getCode());
            stillStale.add(positionKey(entry.getWarehouse(), entry.getLocation()));
        }

        for (StockExit exit : stockExitRepository.findByStatusAndCreatedAtBefore(StockExitStatus.DRAFT, cutoff)) {
            long ageHours = ChronoUnit.HOURS.between(exit.getCreatedAt(), Instant.now());
            trigger(exit.getWarehouse(), exit.getLocation(), hours, ageHours,
                    "Sortie " + exit.getExitNumber() + " en brouillon depuis " + ageHours
                            + "h (creee par " + exit.getCreatedBy() + ") — entrepot "
                            + exit.getWarehouse().getCode());
            stillStale.add(positionKey(exit.getWarehouse(), exit.getLocation()));
        }

        for (Alert open : alertRepository.findByTypeAndStatus(AlertType.STALE_STOCK_DRAFT, AlertStatus.OPEN)) {
            if (!stillStale.contains(positionKey(open.getWarehouse(), open.getLocation()))) {
                alertService.autoResolveIfOpen(AlertType.STALE_STOCK_DRAFT,
                        new AlertService.AlertPosition(null, open.getWarehouse(), open.getLocation(), null));
            }
        }
    }

    private void trigger(Warehouse warehouse, Location location, long thresholdHours, long ageHours, String message) {
        AlertService.AlertPosition position = new AlertService.AlertPosition(null, warehouse, location, null);
        alertService.triggerIfNeeded(AlertType.STALE_STOCK_DRAFT, AlertSeverity.WARNING, position,
                BigDecimal.valueOf(ageHours), BigDecimal.valueOf(thresholdHours), message);
    }

    private static String positionKey(Warehouse warehouse, Location location) {
        Long wid = warehouse != null ? warehouse.getId() : null;
        Long lid = location != null ? location.getId() : null;
        return wid + ":" + lid;
    }
}
