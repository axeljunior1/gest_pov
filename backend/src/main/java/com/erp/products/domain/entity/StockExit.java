package com.erp.products.domain.entity;

import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_exits", indexes = {
        @Index(name = "idx_stock_exits_status", columnList = "status"),
        @Index(name = "idx_stock_exits_exit_date", columnList = "exit_date"),
        @Index(name = "idx_stock_exits_reason", columnList = "reason")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockExit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exit_number", nullable = false, unique = true, length = 50)
    private String exitNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockExitReason reason;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StockExitStatus status = StockExitStatus.DRAFT;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "validated_by", length = 100)
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "stockExit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockExitLine> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (exitDate == null) {
            exitDate = LocalDate.now();
        }
    }
}
