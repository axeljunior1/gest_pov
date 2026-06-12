package com.erp.products.service.alert;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.AlertRepository;
import com.erp.products.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository ruleRepository;
    private final NotificationService notificationService;

    public record AlertPosition(
            Product product,
            Warehouse warehouse,
            Location location,
            Lot lot) {

        public Long lotKey() {
            return lot != null ? lot.getId() : 0L;
        }
    }

    @Transactional(readOnly = true)
    public List<Alert> findOpen() {
        return alertRepository.findByStatusOrderByLastTriggeredAtDesc(AlertStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<Alert> findAll() {
        return alertRepository.findAllByOrderByLastTriggeredAtDesc();
    }

    @Transactional(readOnly = true)
    public Alert getById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee: " + id));
    }

    @Transactional
    public Alert triggerIfNeeded(
            AlertType type,
            AlertSeverity severity,
            AlertPosition position,
            BigDecimal triggeredValue,
            BigDecimal thresholdValue,
            String message) {

        if (!isRuleEnabled(type)) {
            return null;
        }

        Long productId = position.product() != null ? position.product().getId() : null;
        Long warehouseId = position.warehouse() != null ? position.warehouse().getId() : null;
        Long locationId = position.location() != null ? position.location().getId() : null;
        Long lotKey = position.lotKey();

        var existing = alertRepository.findByTypeAndProductIdAndWarehouseIdAndLocationIdAndLotKeyAndStatus(
                type, productId, warehouseId, locationId, lotKey, AlertStatus.OPEN);

        if (existing.isPresent()) {
            Alert alert = existing.get();
            alert.setLastTriggeredAt(Instant.now());
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alert.setTriggeredValue(triggeredValue);
            alert.setThresholdValue(thresholdValue);
            alert.setMessage(message);
            return alertRepository.save(alert);
        }

        Alert alert = Alert.builder()
                .type(type)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .product(position.product())
                .warehouse(position.warehouse())
                .location(position.location())
                .lot(position.lot())
                .lotKey(lotKey)
                .message(message)
                .triggeredValue(triggeredValue)
                .thresholdValue(thresholdValue)
                .firstTriggeredAt(Instant.now())
                .lastTriggeredAt(Instant.now())
                .triggerCount(1)
                .build();

        Alert saved = alertRepository.save(alert);
        notificationService.dispatchForAlert(saved);
        return saved;
    }

    @Transactional
    public void autoResolveIfOpen(AlertType type, AlertPosition position) {
        Long productId = position.product() != null ? position.product().getId() : null;
        Long warehouseId = position.warehouse() != null ? position.warehouse().getId() : null;
        Long locationId = position.location() != null ? position.location().getId() : null;

        alertRepository.findByTypeAndProductIdAndWarehouseIdAndLocationIdAndLotKeyAndStatus(
                        type, productId, warehouseId, locationId, position.lotKey(), AlertStatus.OPEN)
                .ifPresent(a -> resolve(a, "system"));
    }

    @Transactional
    public Alert acknowledge(Long id, String user) {
        Alert alert = getById(id);
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new BusinessException("Seule une alerte OPEN peut etre acquittee");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolve(Long id, String user) {
        return resolve(getById(id), user);
    }

    @Transactional
    public Alert ignore(Long id, String user) {
        Alert alert = getById(id);
        alert.setStatus(AlertStatus.IGNORED);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(user);
        return alertRepository.save(alert);
    }

    private Alert resolve(Alert alert, String user) {
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return alert;
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(user);
        return alertRepository.save(alert);
    }

    private boolean isRuleEnabled(AlertType type) {
        return ruleRepository.findByAlertType(type)
                .map(AlertRule::getEnabled)
                .orElse(true);
    }
}
