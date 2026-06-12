package com.erp.products.config;

import com.erp.products.domain.entity.AlertRule;
import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.enums.AlertSettingScope;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.repository.AlertRuleRepository;
import com.erp.products.repository.AlertSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestAlertReferenceDataInitializer implements ApplicationRunner {

    private final AlertRuleRepository ruleRepository;
    private final AlertSettingRepository settingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ruleRepository.count() == 0) {
            seedRules();
        }
        if (settingRepository.count() == 0) {
            settingRepository.save(AlertSetting.builder()
                    .scope(AlertSettingScope.GLOBAL)
                    .maxStockLevel(new BigDecimal("1000"))
                    .dormantDays(90)
                    .actif(true)
                    .build());
        }
    }

    private void seedRules() {
        createRule(AlertType.LOW_STOCK, AlertSeverity.WARNING);
        createRule(AlertType.OUT_OF_STOCK, AlertSeverity.CRITICAL);
        createRule(AlertType.OVERSTOCK, AlertSeverity.INFO);
        createRule(AlertType.EXPIRY_SOON, AlertSeverity.WARNING);
        createRule(AlertType.EXPIRED, AlertSeverity.CRITICAL);
        createRule(AlertType.DORMANT_PRODUCT, AlertSeverity.INFO);
        createRule(AlertType.SUPPLIER_DELAY, AlertSeverity.WARNING);
        createRule(AlertType.INVENTORY_DISCREPANCY, AlertSeverity.WARNING);
    }

    private void createRule(AlertType type, AlertSeverity severity) {
        ruleRepository.save(AlertRule.builder()
                .alertType(type)
                .defaultSeverity(severity)
                .enabled(true)
                .description(type.name())
                .build());
    }
}
