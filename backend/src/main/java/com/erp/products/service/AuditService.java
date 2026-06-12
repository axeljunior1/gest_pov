package com.erp.products.service;

import com.erp.products.domain.entity.AuditLog;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.repository.AuditLogRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String DEFAULT_USER = "system";

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void log(String entityType, Long entityId, AuditAction action, String details) {
        log(entityType, entityId, action, details, currentUserService.getCurrentUserEmailOrDefault());
    }

    @Transactional
    public void log(String entityType, Long entityId, AuditAction action, String details, String utilisateur) {
        String actor = currentUserService.resolveActor(utilisateur);
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .details(details)
                .utilisateur(actor != null && !actor.isBlank() ? actor : DEFAULT_USER)
                .build();
        auditLogRepository.save(log);
    }
}
