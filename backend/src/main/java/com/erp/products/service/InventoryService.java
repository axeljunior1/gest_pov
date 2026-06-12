package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AuditAction;
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
import com.erp.products.specification.InventoryCountSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryCountRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final LotRepository lotRepository;
    private final StockItemRepository stockItemRepository;
    private final StockLedgerService ledger;
    private final StockService stockService;
    private final StockMapper mapper;
    private final AlertRuleEngine alertRuleEngine;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final SettingsService settingsService;

    @Transactional
    public InventoryCountResponse create(InventoryCountRequest request) {
        String actor = resolveActor(request.getCreatedBy());
        String number = generateInventoryNumber();
        InventoryCount count = InventoryCount.builder()
                .inventoryNumber(number)
                .reference(number)
                .warehouse(loadWarehouse(request.getWarehouseId()))
                .location(loadLocationOptional(request.getWarehouseId(), request.getLocationId()))
                .status(InventoryCountStatus.DRAFT)
                .notes(request.getNotes())
                .createdBy(actor)
                .lignes(new ArrayList<>())
                .build();

        count.getLignes().addAll(buildLines(count, request.getLignes()));
        InventoryCount saved = inventoryRepository.save(count);
        auditService.log("InventoryCount", saved.getId(), AuditAction.CREATION,
                "Inventaire brouillon " + saved.getInventoryNumber(), actor);
        return mapper.toInventoryResponse(saved);
    }

    @Transactional
    public InventoryCountResponse update(Long id, InventoryCountRequest request) {
        InventoryCount count = findCount(id);
        ensureEditable(count);

        count.setWarehouse(loadWarehouse(request.getWarehouseId()));
        count.setLocation(loadLocationOptional(request.getWarehouseId(), request.getLocationId()));
        count.setNotes(request.getNotes());
        count.getLignes().clear();
        count.getLignes().addAll(buildLines(count, request.getLignes()));

        InventoryCount saved = inventoryRepository.save(count);
        auditService.log("InventoryCount", saved.getId(), AuditAction.MODIFICATION,
                "Inventaire modifié " + saved.getInventoryNumber(), request.getCreatedBy());
        return mapper.toInventoryResponse(saved);
    }

    @Transactional
    public InventoryCountResponse start(Long id, String user) {
        InventoryCount count = findCount(id);
        if (count.getStatus() != InventoryCountStatus.DRAFT) {
            throw new BusinessException("Seul un inventaire brouillon peut être démarré");
        }
        if (count.getLignes().isEmpty()) {
            throw new BusinessException("L'inventaire doit contenir au moins une ligne");
        }
        refreshSystemQuantities(count);
        count.setStatus(InventoryCountStatus.IN_PROGRESS);
        count.setStartedAt(Instant.now());
        InventoryCount saved = inventoryRepository.save(count);
        auditService.log("InventoryCount", saved.getId(), AuditAction.MODIFICATION,
                "Inventaire démarré " + saved.getInventoryNumber(), user);
        return mapper.toInventoryResponse(saved);
    }

    @Transactional
    public InventoryCountResponse validate(Long id, String user) {
        InventoryCount count = inventoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventaire non trouvé: " + id));

        if (count.getStatus() == InventoryCountStatus.VALIDATED) {
            throw new BusinessException("Inventaire déjà validé");
        }
        if (count.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new BusinessException("Inventaire annulé");
        }
        if (count.getStatus() != InventoryCountStatus.IN_PROGRESS && count.getStatus() != InventoryCountStatus.DRAFT) {
            throw new BusinessException("Statut invalide pour validation");
        }
        if (count.getLignes().isEmpty()) {
            throw new BusinessException("L'inventaire doit contenir au moins une ligne");
        }

        String actor = resolveActor(user);
        refreshSystemQuantities(count);

        for (InventoryCountLine line : count.getLignes()) {
            BigDecimal ecart = line.getQuantityCounted().subtract(line.getQuantitySystem());
            line.setEcart(ecart);
            if (ecart.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            StockOperationRequest op = new StockOperationRequest();
            op.setProductId(line.getProduct().getId());
            op.setVariantId(line.getVariant() != null ? line.getVariant().getId() : null);
            op.setWarehouseId(count.getWarehouse().getId());
            op.setLocationId(line.getLocation().getId());
            op.setLotId(line.getLot() != null ? line.getLot().getId() : null);
            op.setQuantityBase(ecart);
            op.setReferenceType("INVENTORY_COUNT");
            op.setReference(count.getInventoryNumber());
            op.setReason("Écart inventaire " + count.getInventoryNumber());
            op.setUtilisateur(actor);

            alertRuleEngine.onInventoryDiscrepancy(line, count.getInventoryNumber());

            ledger.applyOnHandChange(
                    op.getProductId(),
                    op.getVariantId(),
                    op.getWarehouseId(),
                    op.getLocationId(),
                    op.getLotId(),
                    ecart,
                    StockService.meta(StockMovementType.INVENTORY, op, null, null, count.getId()));
        }

        count.setStatus(InventoryCountStatus.VALIDATED);
        count.setValidatedBy(actor);
        count.setValidatedAt(Instant.now());
        count.setCompletedAt(Instant.now());

        InventoryCount saved = inventoryRepository.save(count);
        auditService.log("InventoryCount", saved.getId(), AuditAction.MODIFICATION,
                "Inventaire validé " + saved.getInventoryNumber(), user);
        return mapper.toInventoryResponse(saved);
    }

    @Transactional
    public InventoryCountResponse cancel(Long id, String user) {
        InventoryCount count = inventoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventaire non trouvé: " + id));

        if (count.getStatus() == InventoryCountStatus.CANCELLED) {
            throw new BusinessException("Inventaire déjà annulé");
        }
        if (count.getStatus() == InventoryCountStatus.VALIDATED) {
            throw new BusinessException("Un inventaire validé ne peut pas être annulé");
        }

        String actor = resolveActor(user);
        count.setStatus(InventoryCountStatus.CANCELLED);
        count.setCancelledBy(actor);
        count.setCancelledAt(Instant.now());

        InventoryCount saved = inventoryRepository.save(count);
        auditService.log("InventoryCount", saved.getId(), AuditAction.MODIFICATION,
                "Inventaire annulé " + saved.getInventoryNumber(), user);
        return mapper.toInventoryResponse(saved);
    }

    @Transactional
    public void deleteLine(Long inventoryId, Long lineId) {
        InventoryCount count = findCount(inventoryId);
        ensureEditable(count);
        boolean removed = count.getLignes().removeIf(l -> l.getId() != null && l.getId().equals(lineId));
        if (!removed) {
            throw new ResourceNotFoundException("Ligne non trouvée: " + lineId);
        }
        inventoryRepository.save(count);
    }

    @Transactional
    public void delete(Long id) {
        InventoryCount count = findCount(id);
        ensureEditable(count);
        inventoryRepository.delete(count);
        auditService.log("InventoryCount", id, AuditAction.SUPPRESSION,
                "Inventaire brouillon supprimé " + count.getInventoryNumber(), count.getCreatedBy());
    }

    @Transactional(readOnly = true)
    public List<InventoryCountResponse> findAll(
            Long warehouseId,
            Long locationId,
            InventoryCountStatus status,
            LocalDate dateFrom,
            LocalDate dateTo) {
        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1) : null;
        return inventoryRepository.findAll(InventoryCountSpecification.withFilters(
                        warehouseId, locationId, status, from, to))
                .stream()
                .map(mapper::toInventoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse findById(Long id) {
        return mapper.toInventoryResponse(findCount(id));
    }

    private List<InventoryCountLine> buildLines(InventoryCount count, List<InventoryCountRequest.Line> lineReqs) {
        List<InventoryCountLine> lines = new ArrayList<>();
        for (InventoryCountRequest.Line req : lineReqs) {
            Product product = loadProduct(req.getProductId());
            ProductVariant variant = resolveVariant(req.getProductId(), req.getVariantId());
            Long lineLocationId = req.getLocationId() != null
                    ? req.getLocationId()
                    : (count.getLocation() != null ? count.getLocation().getId() : null);
            if (lineLocationId == null) {
                throw new BusinessException("Emplacement obligatoire (en-tête ou ligne)");
            }
            Location location = loadLocation(count.getWarehouse().getId(), lineLocationId);
            Lot lot = loadLot(req.getLotId());
            ProductPackaging packaging = loadPackagingOptional(req.getProductId(), req.getPackagingId());
            BigDecimal countedBase = resolveCountedBase(req);

            Long lotKey = lot != null ? lot.getId() : 0L;
            BigDecimal systemQty = readSystemQuantity(
                    product.getId(),
                    variant != null ? variant.getId() : null,
                    count.getWarehouse().getId(),
                    location.getId(),
                    lotKey);

            lines.add(InventoryCountLine.builder()
                    .inventoryCount(count)
                    .product(product)
                    .variant(variant)
                    .location(location)
                    .lot(lot)
                    .packaging(packaging)
                    .quantitySystem(systemQty)
                    .quantityInput(req.getQuantityInput())
                    .quantityCounted(countedBase)
                    .ecart(countedBase.subtract(systemQty))
                    .notes(req.getNotes())
                    .build());
        }
        return lines;
    }

    private void refreshSystemQuantities(InventoryCount count) {
        for (InventoryCountLine line : count.getLignes()) {
            Long lotKey = line.getLot() != null ? line.getLot().getId() : 0L;
            BigDecimal systemQty = readSystemQuantity(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    count.getWarehouse().getId(),
                    line.getLocation().getId(),
                    lotKey);
            line.setQuantitySystem(systemQty);
        }
    }

    private BigDecimal readSystemQuantity(
            Long productId, Long variantId, Long warehouseId, Long locationId, Long lotKey) {
        return stockItemRepository.findByPosition(productId, variantId, warehouseId, locationId, lotKey)
                .map(StockItem::getQuantityOnHand)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal resolveCountedBase(InventoryCountRequest.Line line) {
        if (line.getPackagingId() != null) {
            StockOperationRequest op = new StockOperationRequest();
            op.setProductId(line.getProductId());
            op.setPackagingId(line.getPackagingId());
            op.setPackagingQuantity(line.getQuantityInput());
            return stockService.resolveBaseQuantity(op);
        }
        if (line.getQuantityInput() == null || line.getQuantityInput().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La quantité comptée doit être positive ou nulle");
        }
        return line.getQuantityInput();
    }

    private String generateInventoryNumber() {
        String prefix = settingsService.getNumberingConfig().getInventoryPrefix()
                + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = inventoryRepository.countByInventoryNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private void ensureEditable(InventoryCount count) {
        if (count.getStatus() != InventoryCountStatus.DRAFT
                && count.getStatus() != InventoryCountStatus.IN_PROGRESS) {
            throw new BusinessException("Seul un inventaire brouillon ou en cours peut être modifié");
        }
    }

    private InventoryCount findCount(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventaire non trouvé: " + id));
    }

    private String resolveActor(String fallback) {
        return currentUserService.resolveActor(fallback);
    }

    private Warehouse loadWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé: " + id));
    }

    private Location loadLocationOptional(Long warehouseId, Long locationId) {
        if (locationId == null) {
            return null;
        }
        return loadLocation(warehouseId, locationId);
    }

    private Location loadLocation(Long warehouseId, Long locationId) {
        return locationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Emplacement non trouvé"));
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + id));
    }

    private ProductVariant resolveVariant(Long productId, Long variantId) {
        if (variantId != null) {
            return loadVariant(productId, variantId);
        }
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        if (variants.size() == 1) {
            return variants.get(0);
        }
        return null;
    }

    private ProductVariant loadVariant(Long productId, Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée: " + variantId));
        if (!v.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return v;
    }

    private Lot loadLot(Long lotId) {
        if (lotId == null) {
            return null;
        }
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot non trouvé: " + lotId));
    }

    private ProductPackaging loadPackagingOptional(Long productId, Long packagingId) {
        if (packagingId == null) {
            return null;
        }
        return packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouvé"));
    }
}
