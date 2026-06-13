package com.erp.products.domain.entity;

import com.erp.products.domain.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales", indexes = {
        @Index(name = "idx_sales_status", columnList = "status"),
        @Index(name = "idx_sales_session", columnList = "pos_session_id"),
        @Index(name = "idx_sales_number", columnList = "sale_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_number", nullable = false, unique = true, length = 50)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pos_session_id", nullable = false)
    private PosSession posSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SaleStatus status = SaleStatus.DRAFT;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "hold_label", length = 120)
    private String holdLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleLine> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
