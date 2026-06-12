package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.domain.enums.StockEntryStatus;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockEntryRequest;
import com.erp.products.dto.StockEntryResponse;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import com.erp.products.specification.StockEntrySpecification;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockEntryService {

    private final StockEntryRepository entryRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final LotRepository lotRepository;
    private final StockLedgerService ledger;
    private final StockService stockService;
    private final StockMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final SettingsService settingsService;

    @Transactional
    public StockEntryResponse create(StockEntryRequest request) {
        StockEntry entry = StockEntry.builder()
                .entryNumber(generateEntryNumber())
                .supplier(loadSupplierOptional(request.getSupplierId()))
                .warehouse(loadWarehouse(request.getWarehouseId()))
                .location(loadLocation(request.getWarehouseId(), request.getLocationId()))
                .entryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now())
                .referenceDocument(request.getReferenceDocument())
                .notes(request.getNotes())
                .status(StockEntryStatus.DRAFT)
                .createdBy(resolveActor(request.getCreatedBy()))
                .lignes(new ArrayList<>())
                .build();

        entry.getLignes().addAll(buildLines(entry, request.getLignes()));
        StockEntry saved = entryRepository.save(entry);
        auditService.log("StockEntry", saved.getId(), AuditAction.CREATION,
                "Entrée stock brouillon " + saved.getEntryNumber(), request.getCreatedBy());
        return mapper.toEntryResponse(saved);
    }

    @Transactional
    public StockEntryResponse update(Long id, StockEntryRequest request) {
        StockEntry entry = findEntry(id);
        ensureDraft(entry);

        entry.setSupplier(loadSupplierOptional(request.getSupplierId()));
        entry.setWarehouse(loadWarehouse(request.getWarehouseId()));
        entry.setLocation(loadLocation(request.getWarehouseId(), request.getLocationId()));
        if (request.getEntryDate() != null) {
            entry.setEntryDate(request.getEntryDate());
        }
        entry.setReferenceDocument(request.getReferenceDocument());
        entry.setNotes(request.getNotes());

        entry.getLignes().clear();
        entry.getLignes().addAll(buildLines(entry, request.getLignes()));

        StockEntry saved = entryRepository.save(entry);
        auditService.log("StockEntry", saved.getId(), AuditAction.MODIFICATION,
                "Entrée stock modifiée " + saved.getEntryNumber(), request.getCreatedBy());
        return mapper.toEntryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StockEntryResponse> findAll(
            Long productId,
            Long supplierId,
            Long warehouseId,
            StockEntryStatus status,
            LocalDate dateFrom,
            LocalDate dateTo) {
        return entryRepository.findAll(StockEntrySpecification.withFilters(
                        productId, supplierId, warehouseId, status, dateFrom, dateTo))
                .stream()
                .map(mapper::toEntryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockEntryResponse findById(Long id) {
        return mapper.toEntryResponse(findEntry(id));
    }

    @Transactional
    public StockEntryResponse validate(Long id, String user) {
        StockEntry entry = entryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée stock non trouvée: " + id));

        if (entry.getStatus() != StockEntryStatus.DRAFT) {
            throw new BusinessException("Seule une entrée brouillon peut être validée");
        }
        if (entry.getLignes().isEmpty()) {
            throw new BusinessException("L'entrée doit contenir au moins une ligne");
        }

        for (StockEntryLine line : entry.getLignes()) {
            postMovement(entry, line, user, StockMovementType.IN, line.getQuantityInBaseUnit());
        }

        entry.setStatus(StockEntryStatus.VALIDATED);
        entry.setValidatedBy(resolveActor(user));
        entry.setValidatedAt(Instant.now());

        StockEntry saved = entryRepository.save(entry);
        auditService.log("StockEntry", saved.getId(), AuditAction.MODIFICATION,
                "Entrée stock validée " + saved.getEntryNumber(), user);
        return mapper.toEntryResponse(saved);
    }

    @Transactional
    public StockEntryResponse cancel(Long id, String user) {
        StockEntry entry = entryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée stock non trouvée: " + id));

        if (entry.getStatus() == StockEntryStatus.CANCELLED) {
            throw new BusinessException("Entrée déjà annulée");
        }
        if (entry.getStatus() == StockEntryStatus.VALIDATED) {
            for (StockEntryLine line : entry.getLignes()) {
                ensureAvailableForCancel(entry, line);
                postMovement(entry, line, user, StockMovementType.OUT, line.getQuantityInBaseUnit().negate());
            }
        }

        entry.setStatus(StockEntryStatus.CANCELLED);
        entry.setCancelledBy(resolveActor(user));
        entry.setCancelledAt(Instant.now());

        StockEntry saved = entryRepository.save(entry);
        auditService.log("StockEntry", saved.getId(), AuditAction.MODIFICATION,
                "Entrée stock annulée " + saved.getEntryNumber(), user);
        return mapper.toEntryResponse(saved);
    }

    @Transactional
    public void deleteLine(Long entryId, Long lineId) {
        StockEntry entry = findEntry(entryId);
        ensureDraft(entry);
        boolean removed = entry.getLignes().removeIf(l -> l.getId().equals(lineId));
        if (!removed) {
            throw new ResourceNotFoundException("Ligne non trouvée: " + lineId);
        }
        entryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        StockEntry entry = findEntry(id);
        ensureDraft(entry);
        entryRepository.delete(entry);
        auditService.log("StockEntry", id, AuditAction.SUPPRESSION,
                "Entrée stock brouillon supprimée " + entry.getEntryNumber(), entry.getCreatedBy());
    }

    private void postMovement(
            StockEntry entry,
            StockEntryLine line,
            String user,
            StockMovementType type,
            BigDecimal delta) {

        Lot lot = resolveLot(line);
        Long lotId = lot != null ? lot.getId() : null;

        StockOperationRequest op = new StockOperationRequest();
        op.setProductId(line.getProduct().getId());
        op.setVariantId(line.getVariant() != null ? line.getVariant().getId() : null);
        op.setWarehouseId(entry.getWarehouse().getId());
        op.setLocationId(entry.getLocation().getId());
        op.setLotId(lotId);
        op.setReferenceType("STOCK_ENTRY");
        op.setReference(entry.getEntryNumber());
        op.setReason(type == StockMovementType.IN
                ? "Entrée stock " + entry.getEntryNumber()
                : "Annulation entrée " + entry.getEntryNumber());
        op.setUtilisateur(resolveActor(user));

        ledger.applyOnHandChange(
                op.getProductId(),
                op.getVariantId(),
                op.getWarehouseId(),
                op.getLocationId(),
                op.getLotId(),
                delta,
                StockService.meta(type, op, null, null, null, entry.getId(), null));
    }

    private void ensureAvailableForCancel(StockEntry entry, StockEntryLine line) {
        BigDecimal available = ledger.getAvailable(
                line.getProduct().getId(),
                line.getVariant() != null ? line.getVariant().getId() : null,
                entry.getWarehouse().getId());
        if (available.compareTo(line.getQuantityInBaseUnit()) < 0) {
            throw new BusinessException("Stock insuffisant pour annuler l'entrée — produit "
                    + line.getProduct().getNom());
        }
    }

    private List<StockEntryLine> buildLines(StockEntry entry, List<StockEntryRequest.Line> lineReqs) {
        List<StockEntryLine> lines = new ArrayList<>();
        for (StockEntryRequest.Line req : lineReqs) {
            Product product = loadProduct(req.getProductId());
            ProductVariant variant = resolveVariant(req.getProductId(), req.getVariantId());
            ProductPackaging packaging = loadPackagingOptional(req.getProductId(), req.getPackagingId());
            BigDecimal baseQty = resolveLineBaseQuantity(req);

            lines.add(StockEntryLine.builder()
                    .stockEntry(entry)
                    .product(product)
                    .variant(variant)
                    .packaging(packaging)
                    .quantityInput(req.getQuantityInput())
                    .quantityInBaseUnit(baseQty)
                    .unitCost(req.getUnitCost())
                    .lotNumber(req.getLotNumber())
                    .expiryDate(req.getExpiryDate())
                    .notes(req.getNotes())
                    .build());
        }
        return lines;
    }

    private BigDecimal resolveLineBaseQuantity(StockEntryRequest.Line line) {
        if (line.getPackagingId() != null) {
            StockOperationRequest op = new StockOperationRequest();
            op.setProductId(line.getProductId());
            op.setPackagingId(line.getPackagingId());
            op.setPackagingQuantity(line.getQuantityInput());
            return stockService.resolveBaseQuantity(op);
        }
        if (line.getQuantityInput() == null || line.getQuantityInput().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantité doit être positive");
        }
        return line.getQuantityInput();
    }

    private Lot resolveLot(StockEntryLine line) {
        if (line.getLotNumber() == null || line.getLotNumber().isBlank()) {
            return null;
        }
        Product product = line.getProduct();
        ProductVariant variant = line.getVariant();
        String numero = line.getLotNumber().trim();

        var existing = variant != null
                ? lotRepository.findByProductIdAndVariantIdAndNumeroLot(product.getId(), variant.getId(), numero)
                : lotRepository.findByProductIdAndVariantIsNullAndNumeroLot(product.getId(), numero);

        if (existing.isPresent()) {
            Lot lot = existing.get();
            if (line.getExpiryDate() != null && lot.getDatePeremption() == null) {
                lot.setDatePeremption(line.getExpiryDate());
                lotRepository.save(lot);
            }
            return lot;
        }

        return lotRepository.save(Lot.builder()
                .product(product)
                .variant(variant)
                .numeroLot(numero)
                .datePeremption(line.getExpiryDate())
                .build());
    }

    private String generateEntryNumber() {
        String prefix = settingsService.getNumberingConfig().getEntryPrefix()
                + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = entryRepository.countByEntryNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private void ensureDraft(StockEntry entry) {
        if (entry.getStatus() != StockEntryStatus.DRAFT) {
            throw new BusinessException("Seule une entrée brouillon peut être modifiée");
        }
    }

    private String resolveActor(String fallback) {
        return currentUserService.resolveActor(fallback);
    }

    private StockEntry findEntry(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée stock non trouvée: " + id));
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
        if (variantId == null) {
            return null;
        }
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée: " + variantId));
        if (!v.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return v;
    }

    private Warehouse loadWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé: " + id));
    }

    private Location loadLocation(Long warehouseId, Long locationId) {
        return locationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Emplacement non trouvé"));
    }

    private Supplier loadSupplierOptional(Long id) {
        if (id == null) {
            return null;
        }
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé: " + id));
    }

    private ProductPackaging loadPackagingOptional(Long productId, Long packagingId) {
        if (packagingId == null) {
            return null;
        }
        return packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouvé"));
    }
}
