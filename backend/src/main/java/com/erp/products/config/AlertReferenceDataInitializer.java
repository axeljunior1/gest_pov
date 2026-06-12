package com.erp.products.config;

import com.erp.products.domain.entity.AlertRule;
import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.entity.UserNotificationPreference;
import com.erp.products.domain.enums.*;
import com.erp.products.repository.AlertRuleRepository;
import com.erp.products.repository.AlertSettingRepository;
import com.erp.products.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AlertReferenceDataInitializer implements ApplicationRunner {

    private final AlertRuleRepository ruleRepository;
    private final AlertSettingRepository settingRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ruleRepository.count() == 0) {
            log.info("Initialisation des regles d'alerte...");
            seedRules();
        }
        if (settingRepository.count() == 0) {
            log.info("Initialisation des parametres d'alerte globaux...");
            settingRepository.save(AlertSetting.builder()
                    .scope(AlertSettingScope.GLOBAL)
                    .maxStockLevel(new BigDecimal("1000"))
                    .dormantDays(90)
                    .actif(true)
                    .build());
        }
        if (preferenceRepository.count() == 0) {
            log.info("Initialisation des preferences de notification admin...");
            for (AlertType type : AlertType.values()) {
                preferenceRepository.save(UserNotificationPreference.builder()
                        .userId("admin@erp.local")
                        .alertType(type)
                        .channel(NotificationChannel.IN_APP)
                        .enabled(true)
                        .build());
            }
        }
    }

    private void seedRules() {
        createRule(AlertType.LOW_STOCK, AlertSeverity.WARNING, "Stock faible");
        createRule(AlertType.OUT_OF_STOCK, AlertSeverity.CRITICAL, "Rupture de stock");
        createRule(AlertType.OVERSTOCK, AlertSeverity.INFO, "Surstock");
        createRule(AlertType.EXPIRY_SOON, AlertSeverity.WARNING, "Péremption proche");
        createRule(AlertType.EXPIRED, AlertSeverity.CRITICAL, "Produit expiré");
        createRule(AlertType.DORMANT_PRODUCT, AlertSeverity.INFO, "Produit dormant");
        createRule(AlertType.SUPPLIER_DELAY, AlertSeverity.WARNING, "Retard fournisseur");
        createRule(AlertType.INVENTORY_DISCREPANCY, AlertSeverity.WARNING, "Écart d'inventaire");
    }

    private void createRule(AlertType type, AlertSeverity severity, String description) {
        ruleRepository.save(AlertRule.builder()
                .alertType(type)
                .defaultSeverity(severity)
                .enabled(true)
                .description(description)
                .build());
    }
}
