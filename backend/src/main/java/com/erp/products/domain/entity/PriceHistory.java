package com.erp.products.domain.entity;

import com.erp.products.domain.enums.PriceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceType type;

    @Column(precision = 19, scale = 4)
    private BigDecimal ancienPrix;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal nouveauPrix;

    @Column(nullable = false)
    private String utilisateur;

    @Column(nullable = false, updatable = false)
    private Instant dateModification;

    @PrePersist
    void onCreate() {
        dateModification = Instant.now();
    }
}
