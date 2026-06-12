package com.erp.products.domain.entity;

import com.erp.products.domain.enums.AlertSeverity;
import com.erp.products.domain.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "alert_rules", uniqueConstraints = @UniqueConstraint(columnNames = {"alert_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity defaultSeverity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
