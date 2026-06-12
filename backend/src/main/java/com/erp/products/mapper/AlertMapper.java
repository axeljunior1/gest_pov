package com.erp.products.mapper;

import com.erp.products.domain.entity.Alert;
import com.erp.products.domain.entity.Notification;
import com.erp.products.dto.AlertResponse;
import com.erp.products.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toAlertResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .productId(alert.getProduct() != null ? alert.getProduct().getId() : null)
                .productNom(alert.getProduct() != null ? alert.getProduct().getNom() : null)
                .warehouseId(alert.getWarehouse() != null ? alert.getWarehouse().getId() : null)
                .warehouseCode(alert.getWarehouse() != null ? alert.getWarehouse().getCode() : null)
                .locationId(alert.getLocation() != null ? alert.getLocation().getId() : null)
                .locationCode(alert.getLocation() != null ? alert.getLocation().getCode() : null)
                .lotId(alert.getLot() != null ? alert.getLot().getId() : null)
                .lotNumero(alert.getLot() != null ? alert.getLot().getNumeroLot() : null)
                .message(alert.getMessage())
                .triggeredValue(alert.getTriggeredValue())
                .thresholdValue(alert.getThresholdValue())
                .firstTriggeredAt(alert.getFirstTriggeredAt())
                .lastTriggeredAt(alert.getLastTriggeredAt())
                .triggerCount(alert.getTriggerCount())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy())
                .build();
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        Alert alert = notification.getAlert();
        return NotificationResponse.builder()
                .id(notification.getId())
                .alertId(alert.getId())
                .alertType(alert.getType())
                .alertMessage(alert.getMessage())
                .userId(notification.getUserId())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .errorMessage(notification.getErrorMessage())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
