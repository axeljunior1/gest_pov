package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_valuation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "stock_value", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal stockValue = BigDecimal.ZERO;

    @Column(name = "average_unit_cost", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal averageUnitCost = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }
}
