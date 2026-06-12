package com.erp.products.config;

import com.erp.products.service.alert.AlertRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduledTask {

    private final AlertRuleEngine alertRuleEngine;

    @Scheduled(cron = "${erp.alerts.daily-cron:0 0 6 * * *}")
    public void runDailyAlertChecks() {
        log.info("Exécution des contrôles d'alerte planifiés...");
        alertRuleEngine.runScheduledChecks();
    }
}
