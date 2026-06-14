package com.erp.products.domain.entity;

import com.erp.products.domain.enums.RefundFulfillmentStatus;
import com.erp.products.domain.enums.SaleRefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_refunds", indexes = {
        @Index(name = "idx_sale_refunds_sale", columnList = "sale_id"),
        @Index(name = "idx_sale_refunds_session", columnList = "pos_session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_number", nullable = false, unique = true, length = 50)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_session_id")
    private PosSession posSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleRefundStatus status = SaleRefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 30)
    @Builder.Default
    private RefundFulfillmentStatus refundStatus = RefundFulfillmentStatus.PENDING;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String reason;

    @Column(length = 500)
    private String notes;

    @Column(name = "return_to_stock", nullable = false)
    @Builder.Default
    private Boolean returnToStock = true;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "manager_validated_by", length = 120)
    private String managerValidatedBy;

    @Column(name = "manager_validated_at")
    private Instant managerValidatedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleRefundLine> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefundPayment> payments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (refundStatus == null) {
            refundStatus = RefundFulfillmentStatus.PENDING;
        }
    }
}
