package com.erp.products.service;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.entity.SaleEvent;
import com.erp.products.domain.entity.User;
import com.erp.products.domain.enums.SaleEventType;
import com.erp.products.repository.SaleEventRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleEventService {

    private final SaleEventRepository saleEventRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void record(Sale sale, SaleEventType type, String description) {
        record(sale, type, description, null, currentUserService.getCurrentUser().orElse(null));
    }

    @Transactional
    public void record(Sale sale, SaleEventType type, String description, String details, User actor) {
        User resolved = actor != null ? actor : currentUserService.getCurrentUser().orElse(null);
        saleEventRepository.save(SaleEvent.builder()
                .sale(sale)
                .eventType(type)
                .description(description != null ? description : type.getLabel())
                .details(details)
                .actor(resolved)
                .actorName(resolved != null ? resolved.fullName() : "Système")
                .build());
    }
}
