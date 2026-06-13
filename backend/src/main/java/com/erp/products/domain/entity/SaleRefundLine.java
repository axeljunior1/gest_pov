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

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount;
}
