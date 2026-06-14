package com.erp.products.domain.entity;

import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String sku;

    private String codeBarre;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marque_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Category categorie;

    @Column(precision = 19, scale = 4)
    private BigDecimal prixAchat;

    @Column(precision = 19, scale = 4)
    private BigDecimal prixVente;

    @Column(precision = 19, scale = 4)
    private BigDecimal prixPromotionnel;

    private Instant prixPromotionnelDebut;

    private Instant prixPromotionnelFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_principal_id")
    private Supplier fournisseurPrincipal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private UnitOfMeasure unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus statut = ProductStatus.ACTIF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LifecycleStatus cycleVie = LifecycleStatus.BROUILLON;

    @Column(name = "is_sellable", nullable = false)
    @Builder.Default
    private Boolean isSellable = true;

    @Column(name = "has_variants", nullable = false)
    @Builder.Default
    private Boolean hasVariants = false;

    @Column(name = "is_stockable", nullable = false)
    @Builder.Default
    private Boolean isStockable = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variantes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductSupplier> fournisseurs = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductCustomAttributeValue> attributs = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductPackaging> conditionnements = new ArrayList<>();

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
