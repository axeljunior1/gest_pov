package com.erp.products.service;

import com.erp.products.domain.entity.AlertSetting;
import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.Warehouse;
import com.erp.products.domain.enums.AlertSettingScope;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.dto.AlertSettingRequest;
import com.erp.products.dto.AlertSettingResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PurchaseOrderMapper;
import com.erp.products.repository.AlertSettingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertSettingService {

    private final AlertSettingRepository settingRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AlertSettingResponse> findAll() {
        return settingRepository.findAll().stream()
                .map(mapper::toAlertSettingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertSettingResponse getById(Long id) {
        return mapper.toAlertSettingResponse(findSetting(id));
    }

    @Transactional
    public AlertSettingResponse create(AlertSettingRequest request) {
        validateScope(request);
        AlertSetting setting = AlertSetting.builder()
                .scope(request.getScope())
                .product(loadProduct(request))
                .warehouse(loadWarehouse(request))
                .minStockLevel(request.getMinStockLevel())
                .maxStockLevel(request.getMaxStockLevel())
                .expiryAlertDays(request.getExpiryAlertDays())
                .dormantDays(request.getDormantDays())
                .actif(request.getActif() == null || request.getActif())
                .build();
        AlertSetting saved = settingRepository.save(setting);
        auditService.log("AlertSetting", saved.getId(), AuditAction.CREATION,
                "Seuil alerte " + saved.getScope());
        return mapper.toAlertSettingResponse(saved);
    }

    @Transactional
    public AlertSettingResponse update(Long id, AlertSettingRequest request) {
        AlertSetting setting = findSetting(id);
        validateScope(request);
        setting.setScope(request.getScope());
        setting.setProduct(loadProduct(request));
        setting.setWarehouse(loadWarehouse(request));
        setting.setMinStockLevel(request.getMinStockLevel());
        setting.setMaxStockLevel(request.getMaxStockLevel());
        setting.setExpiryAlertDays(request.getExpiryAlertDays());
        setting.setDormantDays(request.getDormantDays());
        if (request.getActif() != null) {
            setting.setActif(request.getActif());
        }
        AlertSetting saved = settingRepository.save(setting);
        auditService.log("AlertSetting", saved.getId(), AuditAction.MODIFICATION,
                "Seuil alerte modifié " + saved.getScope());
        return mapper.toAlertSettingResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AlertSetting setting = findSetting(id);
        if (setting.getScope() == AlertSettingScope.GLOBAL) {
            throw new BusinessException("Le seuil global système ne peut pas être supprimé");
        }
        settingRepository.delete(setting);
        auditService.log("AlertSetting", id, AuditAction.SUPPRESSION, "Seuil alerte supprimé");
    }

    private AlertSetting findSetting(Long id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seuil alerte non trouvé: " + id));
    }

    private void validateScope(AlertSettingRequest request) {
        switch (request.getScope()) {
            case GLOBAL -> {
                if (request.getProductId() != null || request.getWarehouseId() != null) {
                    throw new BusinessException("Le scope GLOBAL n'accepte pas produit/entrepôt");
                }
            }
            case PRODUCT -> {
                if (request.getProductId() == null) {
                    throw new BusinessException("Produit obligatoire pour le scope PRODUCT");
                }
                if (request.getWarehouseId() != null) {
                    throw new BusinessException("Entrepôt interdit pour le scope PRODUCT");
                }
            }
            case WAREHOUSE -> {
                if (request.getWarehouseId() == null) {
                    throw new BusinessException("Entrepôt obligatoire pour le scope WAREHOUSE");
                }
                if (request.getProductId() != null) {
                    throw new BusinessException("Produit interdit pour le scope WAREHOUSE");
                }
            }
            case PRODUCT_WAREHOUSE -> {
                if (request.getProductId() == null || request.getWarehouseId() == null) {
                    throw new BusinessException("Produit et entrepôt obligatoires pour PRODUCT_WAREHOUSE");
                }
            }
        }
    }

    private Product loadProduct(AlertSettingRequest request) {
        if (request.getProductId() == null) return null;
        return productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));
    }

    private Warehouse loadWarehouse(AlertSettingRequest request) {
        if (request.getWarehouseId() == null) return null;
        return warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé"));
    }
}
