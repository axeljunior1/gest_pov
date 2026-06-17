package com.erp.products.domain.entity;

import com.erp.products.domain.enums.StockValuationMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_valuation_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 32)
    private StockValuationMovementType movementType;

    @Column(name = "movement_date", nullable = false)
    private Instant movementDate;

    /** Quantité signée : positive = entrée, négative = sortie. */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal totalValue;

    @Column(name = "average_unit_cost_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal averageUnitCostAfter;

    @Column(name = "stock_quantity_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal stockQuantityAfter;

    @Column(name = "stock_value_after", nullable = false, precision = 19, scale = 6)
    private BigDecimal stockValueAfter;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 64)
    private String sourceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
