package com.erp.products.domain.entity;

import com.erp.products.domain.enums.ReferenceValueCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_reference_values", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_ref_category_code", columnNames = {"category", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppReferenceValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReferenceValueCategory category;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
