package com.erp.products.domain.entity;

import com.erp.products.domain.enums.InventoryCountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InventoryCountStatus status = InventoryCountStatus.DRAFT;

    @Column(nullable = false)
    private String utilisateur;

    @OneToMany(mappedBy = "inventoryCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryCountLine> lignes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant validatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
