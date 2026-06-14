package com.erp.products.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variant_attribute_values", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pvav_variant_attribute", columnNames = {"product_variant_id", "attribute_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private VariantAttribute attribute;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private VariantAttributeValue attributeValue;
}
