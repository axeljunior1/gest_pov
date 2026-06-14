package com.erp.products.domain.entity;

import com.erp.products.domain.enums.BarcodeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 255)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(precision = 19, scale = 4)
    private BigDecimal prix;

    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice;

    @Column(name = "is_sellable", nullable = false)
    @Builder.Default
    private Boolean isSellable = true;

    @Column(name = "is_stockable", nullable = false)
    @Builder.Default
    private Boolean isStockable = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    private String codeBarre;

    @Enumerated(EnumType.STRING)
    private BarcodeType barcodeType;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariantAttributeValue> attributeValues = new ArrayList<>();

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
