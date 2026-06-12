package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.InventoryCountStatus;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.InventoryCountRequest;
import com.erp.products.dto.InventoryCountResponse;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.alert.AlertRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryCountRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final StockItemRepository stockItemRepository;
    private final StockLedgerService ledger;
    private final StockMapper mapper;
    private final AlertRuleEngine alertRuleEngine;
    private final CurrentUserService currentUserService;

    @Transactional
    public InventoryCountResponse create(InventoryCountRequest request) {
        if (inventoryRepository.findByReference(request.getReference()).isPresent()) {
            throw new BusinessException("Reference inventaire deja utilisee");
        }
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot non trouve"));

        String actor = currentUserService.resolveActor(request.getUtilisateur());
        InventoryCount count = InventoryCount.builder()
                .reference(request.getReference())
                .warehouse(warehouse)
                .status(InventoryCountStatus.IN_PROGRESS)
                .utilisateur(actor)
                .lignes(new ArrayList<>())
                .build();

        for (InventoryCountRequest.Line lineReq : request.getLignes()) {
            Product product = loadProduct(lineReq.getProductId());
            ProductVariant variant = loadVariant(lineReq.getProductId(), lineReq.getVariantId());
            Location location = loadLocation(warehouse.getId(), lineReq.getLocationId());
            Lot lot = loadLot(lineReq.getLotId());

            Long lotKey = lot != null ? lot.getId() : 0L;
            BigDecimal systemQty = stockItemRepository.findByPosition(
                            product.getId(),
                            variant != null ? variant.getId() : null,
                            warehouse.getId(),
                            location.getId(),
                            lotKey)
                    .map(StockItem::getQuantityOnHand)
                    .orElse(BigDecimal.ZERO);

            BigDecimal ecart = lineReq.getQuantityCounted().subtract(systemQty);
            InventoryCountLine line = InventoryCountLine.builder()
                    .inventoryCount(count)
                    .product(product)
                    .variant(variant)
                    .location(location)
                    .lot(lot)
                    .quantitySystem(systemQty)
                    .quantityCounted(lineReq.getQuantityCounted())
                    .ecart(ecart)
                    .build();
            count.getLignes().add(line);
        }

        return mapper.toInventoryResponse(inventoryRepository.save(count));
    }

    @Transactional
    public InventoryCountResponse validate(Long inventoryId) {
        InventoryCount count = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventaire non trouve"));
        if (count.getStatus() == InventoryCountStatus.VALIDATED) {
            throw new BusinessException("Inventaire deja valide");
        }
        if (count.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new BusinessException("Inventaire annule");
        }

        for (InventoryCountLine line : count.getLignes()) {
            if (line.getEcart().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            StockOperationRequest op = new StockOperationRequest();
            op.setProductId(line.getProduct().getId());
            op.setVariantId(line.getVariant() != null ? line.getVariant().getId() : null);
            op.setWarehouseId(count.getWarehouse().getId());
            op.setLocationId(line.getLocation().getId());
            op.setLotId(line.getLot() != null ? line.getLot().getId() : null);
            op.setQuantityBase(line.getEcart());
            op.setReferenceType("INVENTORY");
            op.setReference(count.getReference());
            op.setReason("Ecart inventaire");
            op.setUtilisateur(currentUserService.resolveActor(count.getUtilisateur()));

            alertRuleEngine.onInventoryDiscrepancy(line, count.getReference());

            ledger.applyOnHandChange(
                    op.getProductId(), op.getVariantId(), op.getWarehouseId(), op.getLocationId(), op.getLotId(),
                    line.getEcart(),
                    StockService.meta(StockMovementType.INVENTORY, op, null, null, count.getId()));
        }

        count.setStatus(InventoryCountStatus.VALIDATED);
        count.setValidatedAt(Instant.now());
        return mapper.toInventoryResponse(inventoryRepository.save(count));
    }

    @Transactional(readOnly = true)
    public List<InventoryCountResponse> findAll() {
        return inventoryRepository.findAll().stream()
                .map(mapper::toInventoryResponse)
                .collect(Collectors.toList());
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve"));
    }

    private ProductVariant loadVariant(Long productId, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvee"));
        if (!v.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide");
        }
        return v;
    }

    private Location loadLocation(Long warehouseId, Long locationId) {
        return locationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Emplacement non trouve"));
    }

    private Lot loadLot(Long lotId) {
        if (lotId == null) {
            return null;
        }
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot non trouve"));
    }
}
