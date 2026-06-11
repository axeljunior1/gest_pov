package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Conversion universelle entre unités physiques (kg↔g, L↔mL…).
 * Réutilisable par tous les produits — ne concerne pas les conditionnements.
 */
@Entity
@Table(name = "unit_conversions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_unit_id", "to_unit_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_unit_id", nullable = false)
    private UnitOfMeasure fromUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_unit_id", nullable = false)
    private UnitOfMeasure toUnit;

    /** 1 unité source = factor × unité cible (ex: 1 kg = 1000 g → factor = 1000) */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal factor;
}
