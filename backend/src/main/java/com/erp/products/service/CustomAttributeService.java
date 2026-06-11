package com.erp.products.service;

import com.erp.products.domain.entity.CustomAttributeDefinition;
import com.erp.products.dto.CustomAttributeDefinitionRequest;
import com.erp.products.dto.CustomAttributeDefinitionResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.CustomAttributeDefinitionRepository;
import com.erp.products.repository.ProductCustomAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomAttributeService {

    private final CustomAttributeDefinitionRepository repository;
    private final ProductCustomAttributeValueRepository valueRepository;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public List<CustomAttributeDefinitionResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toAttributeDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomAttributeDefinitionResponse create(CustomAttributeDefinitionRequest request) {
        if (repository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Code attribut déjà existant: " + request.getCode());
        }
        CustomAttributeDefinition def = CustomAttributeDefinition.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .type(request.getType())
                .build();
        return mapper.toAttributeDefinitionResponse(repository.save(def));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Attribut non trouvé: " + id);
        }
        if (valueRepository.existsByAttributeId(id)) {
            throw new BusinessException("Impossible : des produits utilisent cet attribut");
        }
        repository.deleteById(id);
    }
}
