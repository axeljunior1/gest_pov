package com.erp.products.service;

import com.erp.products.domain.entity.Supplier;
import com.erp.products.dto.SupplierRequest;
import com.erp.products.dto.SupplierResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductSupplierRepository;
import com.erp.products.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAll().stream()
                .map(mapper::toSupplierResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return mapper.toSupplierResponse(findSupplier(id));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> search(String nom) {
        return supplierRepository.findByNomContainingIgnoreCase(nom).stream()
                .map(mapper::toSupplierResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .nom(request.getNom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .build();
        Supplier saved = supplierRepository.save(supplier);
        auditService.log("Supplier", saved.getId(), com.erp.products.domain.enums.AuditAction.CREATION,
                "Fournisseur créé: " + saved.getNom());
        return mapper.toSupplierResponse(saved);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findSupplier(id);
        supplier.setNom(request.getNom());
        supplier.setEmail(request.getEmail());
        supplier.setTelephone(request.getTelephone());
        supplier.setAdresse(request.getAdresse());
        Supplier saved = supplierRepository.save(supplier);
        auditService.log("Supplier", saved.getId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Fournisseur modifié: " + saved.getNom());
        return mapper.toSupplierResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = findSupplier(id);
        if (productRepository.countByFournisseurPrincipalId(id) > 0
                || productSupplierRepository.countBySupplierId(id) > 0) {
            throw new BusinessException("Impossible : ce fournisseur est lié à des produits");
        }
        supplierRepository.delete(supplier);
        auditService.log("Supplier", id, com.erp.products.domain.enums.AuditAction.SUPPRESSION,
                "Fournisseur supprimé: " + supplier.getNom());
    }

    Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé: " + id));
    }
}
