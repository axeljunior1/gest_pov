package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_exit_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockExitLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_exit_id", nullable = false)
    private StockExit stockExit;

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

    @Column(length = 500)
    private String notes;
}
