package com.erp.products.domain.entity;

import com.erp.products.domain.enums.PosSessionStatus;
import com.erp.products.domain.enums.PosSessionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pos_sessions", indexes = {
        @Index(name = "idx_pos_sessions_cashier_status", columnList = "cashier_id, status"),
        @Index(name = "idx_pos_sessions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_number", nullable = false, unique = true, length = 50)
    private String sessionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "opening_cash_amount", precision = 19, scale = 4)
    private BigDecimal openingCashAmount;

    @Column(name = "closing_cash_amount", precision = 19, scale = 4)
    private BigDecimal closingCashAmount;

    @Column(name = "expected_cash_amount", precision = 19, scale = 4)
    private BigDecimal expectedCashAmount;

    @Column(name = "difference_amount", precision = 19, scale = 4)
    private BigDecimal differenceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    @Builder.Default
    private PosSessionType sessionType = PosSessionType.CASHIER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PosSessionStatus status = PosSessionStatus.OPEN;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (openedAt == null) {
            openedAt = Instant.now();
        }
        if (sessionType == null) {
            sessionType = PosSessionType.CASHIER;
        }
    }
}
