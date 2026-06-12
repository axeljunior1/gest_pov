package com.erp.products.service.alert;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class DormantProductChecker {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final AlertSettingResolver settingResolver;
    private final AlertService alertService;

    public void checkAll() {
        Instant now = Instant.now();
        productRepository.findAll().forEach(product -> {
            var settings = settingResolver.resolve(product.getId(), null);
            int dormantDays = settings.dormantDays() != null ? settings.dormantDays() : 90;

            Instant lastMovement = movementRepository.findLastMovementDateByProductId(product.getId())
                    .orElse(product.getCreatedAt());

            long daysSince = ChronoUnit.DAYS.between(lastMovement, now);
            AlertService.AlertPosition position = new AlertService.AlertPosition(product, null, null, null);

            if (daysSince >= dormantDays) {
                alertService.triggerIfNeeded(
                        AlertType.DORMANT_PRODUCT,
                        AlertSeverity.INFO,
                        position,
                        BigDecimal.valueOf(daysSince),
                        BigDecimal.valueOf(dormantDays),
                        "Produit dormant " + product.getNom() + " : aucun mouvement depuis " + daysSince + " jours");
            } else {
                alertService.autoResolveIfOpen(AlertType.DORMANT_PRODUCT, position);
            }
        });
    }
}
