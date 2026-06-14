package com.erp.products.domain.entity;

import com.erp.products.domain.enums.SaleEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sale_events", indexes = {
        @Index(name = "idx_sale_events_sale", columnList = "sale_id, occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SaleEventType eventType;

    @Column(length = 500)
    private String description;

    @Column(length = 2000)
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
