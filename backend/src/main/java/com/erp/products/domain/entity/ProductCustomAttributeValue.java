package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_custom_attribute_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attribute_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCustomAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CustomAttributeDefinition attribute;

    @Column(length = 2000)
    private String valeur;
}
