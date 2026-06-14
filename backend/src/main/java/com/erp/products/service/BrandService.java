package com.erp.products.service;

import com.erp.products.domain.entity.Brand;
import com.erp.products.dto.BrandRequest;
import com.erp.products.dto.BrandResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.BrandRepository;
import com.erp.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BrandResponse> findAll() {
        return brandRepository.findAllByOrderByNomAsc().stream()
                .map(mapper::toBrandResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BrandResponse getById(Long id) {
        return mapper.toBrandResponse(findBrand(id));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> search(String nom) {
        return brandRepository.findByNomContainingIgnoreCaseOrderByNomAsc(nom).stream()
                .map(mapper::toBrandResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String nom = request.getNom().trim();
        if (brandRepository.existsByNomIgnoreCase(nom)) {
            throw new BusinessException("Une marque avec ce nom existe déjà");
        }
        Brand saved = brandRepository.save(Brand.builder().nom(nom).build());
        auditService.log("Brand", saved.getId(), com.erp.products.domain.enums.AuditAction.CREATION,
                "Marque créée: " + saved.getNom());
        return mapper.toBrandResponse(saved);
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = findBrand(id);
        String nom = request.getNom().trim();
        if (!brand.getNom().equalsIgnoreCase(nom) && brandRepository.existsByNomIgnoreCase(nom)) {
            throw new BusinessException("Une marque avec ce nom existe déjà");
        }
        brand.setNom(nom);
        Brand saved = brandRepository.save(brand);
        auditService.log("Brand", saved.getId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Marque modifiée: " + saved.getNom());
        return mapper.toBrandResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = findBrand(id);
        if (productRepository.countByBrand_Id(id) > 0) {
            throw new BusinessException("Impossible : cette marque est liée à des produits");
        }
        brandRepository.delete(brand);
        auditService.log("Brand", id, com.erp.products.domain.enums.AuditAction.SUPPRESSION,
                "Marque supprimée: " + brand.getNom());
    }

    public Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marque non trouvée: " + id));
    }
}
