package com.erp.products.domain.entity;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_status", columnList = "status"),
        @Index(name = "idx_alerts_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "lot_key", nullable = false)
    @Builder.Default
    private Long lotKey = 0L;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_value", precision = 19, scale = 6)
    private BigDecimal triggeredValue;

    @Column(name = "threshold_value", precision = 19, scale = 6)
    private BigDecimal thresholdValue;

    @Column(name = "first_triggered_at", nullable = false)
    private Instant firstTriggeredAt;

    @Column(name = "last_triggered_at", nullable = false)
    private Instant lastTriggeredAt;

    @Column(name = "trigger_count", nullable = false)
    @Builder.Default
    private Integer triggerCount = 1;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        if (firstTriggeredAt == null) {
            firstTriggeredAt = now;
        }
        if (lastTriggeredAt == null) {
            lastTriggeredAt = now;
        }
        if (lotKey == null) {
            lotKey = lot != null ? lot.getId() : 0L;
        }
    }
}
