package com.erp.products.domain.entity;

import com.erp.products.domain.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private String utilisateur;

    @Column(nullable = false, updatable = false)
    private Instant dateAction;

    @PrePersist
    void onCreate() {
        dateAction = Instant.now();
    }
}
