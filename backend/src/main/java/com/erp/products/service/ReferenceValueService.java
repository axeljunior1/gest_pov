package com.erp.products.service;

import com.erp.products.domain.entity.AppReferenceValue;
import com.erp.products.domain.enums.ReferenceValueCategory;
import com.erp.products.dto.ReferenceValueResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.AppReferenceValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReferenceValueService {

    private final AppReferenceValueRepository repository;

    @Transactional(readOnly = true)
    public List<ReferenceValueResponse> listByCategory(ReferenceValueCategory category) {
        return repository.findByCategoryAndActiveTrueOrderBySortOrderAscLabelAsc(category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<ReferenceValueResponse>> listAllGrouped() {
        Map<String, List<ReferenceValueResponse>> grouped = new LinkedHashMap<>();
        for (ReferenceValueCategory category : ReferenceValueCategory.values()) {
            grouped.put(category.name(), listByCategory(category));
        }
        return grouped;
    }

    @Transactional(readOnly = true)
    public void requireValid(ReferenceValueCategory category, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Valeur obligatoire pour " + category.name());
        }
        if (!repository.existsByCategoryAndCodeIgnoreCaseAndActiveTrue(category, code.trim())) {
            throw new BusinessException("Valeur invalide pour " + category.name() + ": " + code);
        }
    }

    private ReferenceValueResponse toResponse(AppReferenceValue value) {
        return ReferenceValueResponse.builder()
                .id(value.getId())
                .category(value.getCategory())
                .code(value.getCode())
                .label(value.getLabel())
                .sortOrder(value.getSortOrder())
                .build();
    }
}
