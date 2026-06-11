package com.erp.products.dto;

import com.erp.products.domain.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditLogResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private AuditAction action;
    private String details;
    private String utilisateur;
    private Instant dateAction;
}
