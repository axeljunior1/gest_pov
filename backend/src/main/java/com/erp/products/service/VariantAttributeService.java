package com.erp.products.service;

import com.erp.products.domain.entity.VariantAttribute;
import com.erp.products.domain.entity.VariantAttributeValue;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.ProductVariantAttributeValueRepository;
import com.erp.products.repository.VariantAttributeRepository;
import com.erp.products.repository.VariantAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantAttributeService {

    private final VariantAttributeRepository attributeRepository;
    private final VariantAttributeValueRepository valueRepository;
    private final ProductVariantAttributeValueRepository variantAttributeValueRepository;

    @Transactional(readOnly = true)
    public List<VariantAttributeResponse> findAll(boolean activeOnly) {
        return attributeRepository.findAll().stream()
                .filter(a -> !activeOnly || Boolean.TRUE.equals(a.getIsActive()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VariantAttributeResponse getById(Long id) {
        return toResponse(findAttribute(id));
    }

    @Transactional
    public VariantAttributeResponse create(VariantAttributeRequest request) {
        String code = normalizeCode(request.getCode() != null ? request.getCode() : request.getName());
        if (attributeRepository.existsByCode(code)) {
            throw new BusinessException("Code attribut déjà existant: " + code);
        }
        VariantAttribute attribute = VariantAttribute.builder()
                .name(request.getName().trim())
                .code(code)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        VariantAttribute saved = attributeRepository.save(attribute);
        if (request.getValues() != null) {
            for (VariantAttributeValueRequest vr : request.getValues()) {
                addValueInternal(saved, vr);
            }
        }
        return toResponse(findAttribute(saved.getId()));
    }

    @Transactional
    public VariantAttributeValueResponse addValue(Long attributeId, VariantAttributeValueRequest request) {
        VariantAttribute attribute = findAttribute(attributeId);
        VariantAttributeValue saved = addValueInternal(attribute, request);
        return toValueResponse(saved);
    }

    @Transactional
    public VariantAttributeResponse update(Long id, VariantAttributeRequest request) {
        VariantAttribute attribute = findAttribute(id);
        if (request.getName() != null) {
            attribute.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            attribute.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            attribute.setIsActive(request.getIsActive());
        }
        return toResponse(attributeRepository.save(attribute));
    }

    @Transactional
    public void delete(Long id) {
        VariantAttribute attribute = findAttribute(id);
        if (variantAttributeValueRepository.existsByAttributeId(id)) {
            throw new BusinessException("Impossible : des variantes utilisent cet attribut");
        }
        attributeRepository.delete(attribute);
    }

    private VariantAttributeValue addValueInternal(VariantAttribute attribute, VariantAttributeValueRequest request) {
        if (request.getValue() == null || request.getValue().isBlank()) {
            throw new BusinessException("Valeur obligatoire");
        }
        String value = request.getValue().trim();
        if (valueRepository.findByAttributeIdAndValueIgnoreCase(attribute.getId(), value).isPresent()) {
            throw new BusinessException("Valeur déjà existante: " + value);
        }
        String code = request.getCode() != null && !request.getCode().isBlank()
                ? normalizeCode(request.getCode())
                : ProductSkuGenerator.normalizePart(value, 12);
        VariantAttributeValue entity = VariantAttributeValue.builder()
                .attribute(attribute)
                .value(value)
                .code(code)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        attribute.getValues().add(entity);
        return valueRepository.save(entity);
    }

    private VariantAttribute findAttribute(Long id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attribut non trouvé: " + id));
    }

    private VariantAttributeResponse toResponse(VariantAttribute attribute) {
        List<VariantAttributeValueResponse> values = valueRepository
                .findByAttributeIdOrderBySortOrderAscValueAsc(attribute.getId()).stream()
                .map(this::toValueResponse)
                .toList();
        return VariantAttributeResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .code(attribute.getCode())
                .description(attribute.getDescription())
                .isActive(attribute.getIsActive())
                .values(values)
                .createdAt(attribute.getCreatedAt())
                .updatedAt(attribute.getUpdatedAt())
                .build();
    }

    private VariantAttributeValueResponse toValueResponse(VariantAttributeValue value) {
        return VariantAttributeValueResponse.builder()
                .id(value.getId())
                .attributeId(value.getAttribute().getId())
                .attributeCode(value.getAttribute().getCode())
                .attributeName(value.getAttribute().getName())
                .value(value.getValue())
                .code(value.getCode())
                .sortOrder(value.getSortOrder())
                .isActive(value.getIsActive())
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt())
                .build();
    }

    private static String normalizeCode(String raw) {
        return ProductSkuGenerator.normalizePart(raw, 30);
    }
}
