package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_entry_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_entry_id", nullable = false)
    private StockEntry stockEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_id")
    private ProductPackaging packaging;

    @Column(name = "quantity_input", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInput;

    @Column(name = "quantity_in_base_unit", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnit;

    @Column(name = "unit_cost", precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 500)
    private String notes;
}
