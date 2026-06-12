package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_count_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_id")
    private ProductPackaging packaging;

    @Column(name = "quantity_system", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantitySystem;

    @Column(name = "quantity_input", precision = 19, scale = 6)
    private BigDecimal quantityInput;

    @Column(name = "quantity_counted", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityCounted;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal ecart;

    @Column(length = 500)
    private String notes;
}
