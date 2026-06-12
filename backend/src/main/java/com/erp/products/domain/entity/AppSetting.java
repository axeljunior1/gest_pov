package com.erp.products.domain.entity;

import com.erp.products.domain.enums.AppSettingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_settings", uniqueConstraints = @UniqueConstraint(columnNames = "setting_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppSettingType type;

    @Column(length = 500)
    private String description;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
