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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    private String nom;

    private String symbole;

    @Column(name = "quantite_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantiteBase;

    private String codeBarre;

    /** Prix de vente pour 1 conditionnement (pas le prix unité de base). */
    @Column(name = "prix_vente", nullable = false, precision = 19, scale = 4)
    private BigDecimal prixVente;

    @Column(name = "prix_achat", precision = 19, scale = 4)
    private BigDecimal prixAchat;

    @Column(name = "default_vente", nullable = false)
    @Builder.Default
    private Boolean defaultVente = false;

    @Column(name = "default_achat", nullable = false)
    @Builder.Default
    private Boolean defaultAchat = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

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
