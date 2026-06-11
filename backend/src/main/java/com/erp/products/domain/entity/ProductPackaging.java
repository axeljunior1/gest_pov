package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Conditionnement spécifique au produit (carton, palette…).
 * quantiteBase = nombre d'unités de base contenues dans 1 conditionnement.
 * Ex: Eau — 1 carton = 12 bouteilles → quantiteBase = 12
 */
@Entity
@Table(name = "product_packagings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPackaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String nom;

    private String symbole;

    @Column(name = "quantite_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantiteBase;

    private String codeBarre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean principal = false;

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
