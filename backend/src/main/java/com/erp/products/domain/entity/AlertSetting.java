package com.erp.products.domain.entity;

import com.erp.products.domain.enums.AlertSettingScope;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alert_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSettingScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "min_stock_level", precision = 19, scale = 6)
    private BigDecimal minStockLevel;

    @Column(name = "max_stock_level", precision = 19, scale = 6)
    private BigDecimal maxStockLevel;

    @Column(name = "expiry_alert_days")
    private Integer expiryAlertDays;

    @Column(name = "dormant_days")
    private Integer dormantDays;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
