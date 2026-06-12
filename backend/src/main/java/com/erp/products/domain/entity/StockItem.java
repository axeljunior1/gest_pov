package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_item_position",
        columnNames = {"product_id", "variant_id", "warehouse_id", "location_id", "lot_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    /** 0 si pas de lot — clé unique stable (évite les NULL en contrainte). */
    @Column(name = "lot_key", nullable = false)
    @Builder.Default
    private Long lotKey = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitOfMeasure unit;

    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (quantityOnHand == null) {
            quantityOnHand = BigDecimal.ZERO;
        }
        if (quantityReserved == null) {
            quantityReserved = BigDecimal.ZERO;
        }
        if (lotKey == null) {
            lotKey = lot != null ? lot.getId() : 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public BigDecimal getQuantityAvailable() {
        return quantityOnHand.subtract(quantityReserved);
    }
}
