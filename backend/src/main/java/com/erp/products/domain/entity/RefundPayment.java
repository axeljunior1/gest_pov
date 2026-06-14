package com.erp.products.domain.entity;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.RefundPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refund_payments", indexes = {
        @Index(name = "idx_refund_payments_session", columnList = "pos_session_id"),
        @Index(name = "idx_refund_payments_refund", columnList = "sale_refund_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_refund_id", nullable = false)
    private SaleRefund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_payment_id")
    private Payment originalPayment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pos_session_id", nullable = false)
    private PosSession posSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefundPaymentStatus status = RefundPaymentStatus.REFUNDED;

    @Column(name = "refunded_at", nullable = false)
    private Instant refundedAt;

    @PrePersist
    void onCreate() {
        if (refundedAt == null) {
            refundedAt = Instant.now();
        }
    }
}
