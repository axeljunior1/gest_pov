package com.erp.products.dto;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AlertResponse {
    private Long id;
    private AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private Long productId;
    private String productNom;
    private Long warehouseId;
    private String warehouseCode;
    private Long locationId;
    private String locationCode;
    private Long lotId;
    private String lotNumero;
    private String message;
    private BigDecimal triggeredValue;
    private BigDecimal thresholdValue;
    private Instant firstTriggeredAt;
    private Instant lastTriggeredAt;
    private Integer triggerCount;
    private Instant createdAt;
    private Instant resolvedAt;
    private String resolvedBy;
}
