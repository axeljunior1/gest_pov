package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_refund_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRefundLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false)
    private SaleRefund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_line_id", nullable = false)
    private SaleLine saleLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_id")
    private ProductPackaging packaging;

    @Column(name = "packaging_name_snapshot")
    private String packagingNameSnapshot;

    @Column(name = "packaging_quantity_snapshot", precision = 19, scale = 6)
    private BigDecimal packagingQuantitySnapshot;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "quantity_in_base_unit", precision = 19, scale = 6)
    private BigDecimal quantityInBaseUnit;

    @Column(name = "unit_price_snapshot", precision = 19, scale = 4)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean restock = true;

    @Column(length = 500)
    private String reason;

    @Column(length = 500)
    private String notes;
}
