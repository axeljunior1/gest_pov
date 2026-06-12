package com.erp.products.domain.entity;

import com.erp.products.domain.enums.ImportJobStatus;
import com.erp.products.domain.enums.ImportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "import_jobs", indexes = {
        @Index(name = "idx_import_jobs_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportType importType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportJobStatus status;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 120)
    private String createdBy;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int successRows;

    @Column(nullable = false)
    private int errorRows;

    @Column(columnDefinition = "TEXT")
    private String errorReport;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
