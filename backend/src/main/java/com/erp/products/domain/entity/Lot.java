package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lots", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "variant_id", "numero_lot"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "numero_lot", nullable = false, length = 100)
    private String numeroLot;

    private LocalDate datePeremption;

    private LocalDate dateFabrication;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
