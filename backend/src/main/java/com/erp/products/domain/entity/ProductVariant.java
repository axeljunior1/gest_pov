package com.erp.products.domain.entity;

import com.erp.products.domain.enums.BarcodeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String couleur;

    private String taille;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(precision = 19, scale = 4)
    private BigDecimal prix;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    private String codeBarre;

    @Enumerated(EnumType.STRING)
    private BarcodeType barcodeType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
