package com.erp.products.domain.entity;

import com.erp.products.domain.enums.InventoryCountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_counts", indexes = {
        @Index(name = "idx_inventory_counts_status", columnList = "status"),
        @Index(name = "idx_inventory_counts_warehouse", columnList = "warehouse_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_number", nullable = false, unique = true, length = 50)
    private String inventoryNumber;

    /** Conservé pour compatibilité mouvements / alertes existants. */
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InventoryCountStatus status = InventoryCountStatus.DRAFT;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(length = 1000)
    private String notes;

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

    @OneToMany(mappedBy = "inventoryCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryCountLine> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (reference == null && inventoryNumber != null) {
            reference = inventoryNumber;
        }
    }
}
