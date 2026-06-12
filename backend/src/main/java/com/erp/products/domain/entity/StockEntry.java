package com.erp.products.domain.entity;

import com.erp.products.domain.enums.StockEntryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_entries", indexes = {
        @Index(name = "idx_stock_entries_status", columnList = "status"),
        @Index(name = "idx_stock_entries_entry_date", columnList = "entry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_number", nullable = false, unique = true, length = 50)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "reference_document", length = 100)
    private String referenceDocument;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StockEntryStatus status = StockEntryStatus.DRAFT;

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

    @OneToMany(mappedBy = "stockEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockEntryLine> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
    }
}
