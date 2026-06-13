package com.erp.products.domain.entity;

import com.erp.products.domain.enums.SaleRefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_refunds")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleRefundStatus status = SaleRefundStatus.PENDING;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String reason;

    @Column(name = "return_to_stock", nullable = false)
    @Builder.Default
    private Boolean returnToStock = true;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleRefundLine> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
