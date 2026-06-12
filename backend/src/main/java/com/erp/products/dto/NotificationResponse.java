package com.erp.products.dto;

import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.NotificationChannel;
import com.erp.products.domain.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long alertId;
    private AlertType alertType;
    private String alertMessage;
    private String userId;
    private NotificationChannel channel;
    private NotificationStatus status;
    private Instant sentAt;
    private Instant readAt;
    private String errorMessage;
    private Instant createdAt;
}
