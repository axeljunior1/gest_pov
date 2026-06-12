package com.erp.products.domain.entity;

import com.erp.products.domain.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movements_product", columnList = "product_id"),
        @Index(name = "idx_stock_movements_date", columnList = "movement_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitOfMeasure unit;

    /** Quantité en unité de base (toujours positive). */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "quantity_on_hand_before", precision = 19, scale = 6)
    private BigDecimal quantityOnHandBefore;

    @Column(name = "quantity_on_hand_after", precision = 19, scale = 6)
    private BigDecimal quantityOnHandAfter;

    @Column(name = "quantity_reserved_before", precision = 19, scale = 6)
    private BigDecimal quantityReservedBefore;

    @Column(name = "quantity_reserved_after", precision = 19, scale = 6)
    private BigDecimal quantityReservedAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private String utilisateur;

    @Column(name = "movement_date", nullable = false)
    private Instant movementDate;

    @Column(name = "stock_transfer_id")
    private Long stockTransferId;

    @Column(name = "stock_reservation_id")
    private Long stockReservationId;

    @Column(name = "inventory_count_id")
    private Long inventoryCountId;

    @Column(name = "stock_entry_id")
    private Long stockEntryId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (movementDate == null) {
            movementDate = createdAt;
        }
    }
}
