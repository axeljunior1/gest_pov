package com.erp.products.service;

import com.erp.products.domain.entity.Category;
import com.erp.products.dto.CategoryRequest;
import com.erp.products.dto.CategoryResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.CategoryRepository;
import com.erp.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        return categoryRepository.findByParentIsNull().stream()
                .map(c -> mapper.toCategoryResponse(c, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return mapper.toCategoryResponse(findCategory(id), true);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> search(String nom) {
        return categoryRepository.searchByNom(nom).stream()
                .map(c -> mapper.toCategoryResponse(c, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .nom(request.getNom())
                .build();

        if (request.getParentId() != null) {
            Category parent = findCategory(request.getParentId());
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        auditService.log("Category", saved.getId(), com.erp.products.domain.enums.AuditAction.CREATION,
                "Catégorie créée: " + saved.getNom());
        return mapper.toCategoryResponse(saved, false);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        category.setNom(request.getNom());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new com.erp.products.exception.BusinessException("Une catégorie ne peut pas être son propre parent");
            }
            category.setParent(findCategory(request.getParentId()));
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        auditService.log("Category", saved.getId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Catégorie modifiée: " + saved.getNom());
        return mapper.toCategoryResponse(saved, false);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException("Supprimez d'abord les sous-catégories");
        }
        if (productRepository.countByCategorieId(id) > 0) {
            throw new BusinessException("Impossible : des produits sont rattachés à cette catégorie");
        }
        categoryRepository.delete(category);
        auditService.log("Category", id, com.erp.products.domain.enums.AuditAction.SUPPRESSION,
                "Catégorie supprimée: " + category.getNom());
    }

    Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée: " + id));
    }
}
