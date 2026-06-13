package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockExitRequest;
import com.erp.products.dto.StockExitResponse;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.specification.StockExitSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
public class StockExitService {

    private final StockExitRepository exitRepository;
    private final SaleRepository saleRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPackagingRepository packagingRepository;
    private final StockLedgerService ledger;
    private final StockService stockService;
    private final StockMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final SettingsService settingsService;

    @Transactional
    public StockExitResponse create(StockExitRequest request) {
        StockExit exit = StockExit.builder()
                .exitNumber(generateExitNumber())
                .warehouse(loadWarehouse(request.getWarehouseId()))
                .location(loadLocation(request.getWarehouseId(), request.getLocationId()))
                .exitDate(request.getExitDate() != null ? request.getExitDate() : LocalDate.now())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(StockExitStatus.DRAFT)
                .createdBy(currentUserService.resolveActor(request.getCreatedBy()))
                .lignes(new ArrayList<>())
                .build();

        exit.getLignes().addAll(buildLines(exit, request.getLignes()));
        StockExit saved = exitRepository.save(exit);
        auditService.log("StockExit", saved.getId(), AuditAction.CREATION,
                "Sortie stock brouillon " + saved.getExitNumber(), request.getCreatedBy());
        return mapper.toExitResponse(saved);
    }

    @Transactional
    public StockExitResponse update(Long id, StockExitRequest request) {
        StockExit exit = findExit(id);
        ensureNotPosLinked(exit);
        ensureDraft(exit);

        exit.setWarehouse(loadWarehouse(request.getWarehouseId()));
        exit.setLocation(loadLocation(request.getWarehouseId(), request.getLocationId()));
        if (request.getExitDate() != null) {
            exit.setExitDate(request.getExitDate());
        }
        exit.setReason(request.getReason());
        exit.setNotes(request.getNotes());

        exit.getLignes().clear();
        exit.getLignes().addAll(buildLines(exit, request.getLignes()));

        StockExit saved = exitRepository.save(exit);
        auditService.log("StockExit", saved.getId(), AuditAction.MODIFICATION,
                "Sortie stock modifiée " + saved.getExitNumber(), request.getCreatedBy());
        return mapper.toExitResponse(saved);
    }

    @Transactional
    public List<StockExitResponse> findAll(
            Long productId,
            Long warehouseId,
            StockExitStatus status,
            StockExitReason reason,
            LocalDate dateFrom,
            LocalDate dateTo) {
        syncMissingPosExits();
        return exitRepository.findAll(StockExitSpecification.withFilters(
                        productId, warehouseId, status, reason, dateFrom, dateTo))
                .stream()
                .map(mapper::toExitResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StockExit createFromPosSale(Sale sale) {
        if (exitRepository.existsBySaleId(sale.getId())) {
            return exitRepository.findBySaleId(sale.getId()).orElseThrow();
        }

        Instant validatedAt = sale.getPaidAt() != null ? sale.getPaidAt() : sale.getValidatedAt();
        if (validatedAt == null) {
            validatedAt = Instant.now();
        }
        LocalDate exitDate = validatedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        String actor = sale.getCashier() != null ? sale.getCashier().getEmail()
                : currentUserService.resolveActor(null);

        StockExit exit = StockExit.builder()
                .exitNumber(generateExitNumber())
                .warehouse(sale.getWarehouse())
                .location(sale.getLocation())
                .exitDate(exitDate)
                .reason(StockExitReason.SALE)
                .notes("Vente POS " + sale.getSaleNumber())
                .status(StockExitStatus.VALIDATED)
                .createdBy(actor)
                .validatedBy(actor)
                .validatedAt(validatedAt)
                .sale(sale)
                .lignes(new ArrayList<>())
                .build();

        for (SaleLine saleLine : sale.getLignes()) {
            exit.getLignes().add(StockExitLine.builder()
                    .stockExit(exit)
                    .product(saleLine.getProduct())
                    .variant(saleLine.getVariant())
                    .packaging(saleLine.getPackaging())
                    .quantityInput(saleLine.getQuantityInput())
                    .quantityInBaseUnit(saleLine.getQuantityInBaseUnit())
                    .build());
        }

        StockExit saved = exitRepository.save(exit);
        auditService.log("StockExit", saved.getId(), AuditAction.CREATION,
                "Sortie stock POS " + saved.getExitNumber() + " (vente " + sale.getSaleNumber() + ")",
                actor);
        return saved;
    }

    @Transactional
    public void syncMissingPosExits() {
        List<Sale> sales = saleRepository.findPaidWithoutStockExit(PageRequest.of(0, 200));
        for (Sale sale : sales) {
            createFromPosSale(sale);
        }
    }

    @Transactional(readOnly = true)
    public StockExitResponse findById(Long id) {
        return mapper.toExitResponse(findExit(id));
    }

    @Transactional
    public StockExitResponse validate(Long id, String user) {
        StockExit exit = exitRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sortie stock non trouvée: " + id));
        ensureNotPosLinked(exit);

        if (exit.getStatus() != StockExitStatus.DRAFT) {
            throw new BusinessException("Seule une sortie brouillon peut être validée");
        }
        if (exit.getLignes().isEmpty()) {
            throw new BusinessException("La sortie doit contenir au moins une ligne");
        }

        for (StockExitLine line : exit.getLignes()) {
            ensureAvailable(exit, line);
            postMovement(exit, line, user, StockMovementType.OUT, line.getQuantityInBaseUnit().negate());
        }

        exit.setStatus(StockExitStatus.VALIDATED);
        exit.setValidatedBy(currentUserService.resolveActor(user));
        exit.setValidatedAt(Instant.now());

        StockExit saved = exitRepository.save(exit);
        auditService.log("StockExit", saved.getId(), AuditAction.MODIFICATION,
                "Sortie stock validée " + saved.getExitNumber(), user);
        return mapper.toExitResponse(saved);
    }

    @Transactional
    public StockExitResponse cancel(Long id, String user) {
        StockExit exit = exitRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sortie stock non trouvée: " + id));
        ensureNotPosLinked(exit);

        if (exit.getStatus() == StockExitStatus.CANCELLED) {
            throw new BusinessException("Sortie déjà annulée");
        }
        if (exit.getStatus() == StockExitStatus.VALIDATED) {
            for (StockExitLine line : exit.getLignes()) {
                postMovement(exit, line, user, StockMovementType.IN, line.getQuantityInBaseUnit());
            }
        }

        exit.setStatus(StockExitStatus.CANCELLED);
        exit.setCancelledBy(currentUserService.resolveActor(user));
        exit.setCancelledAt(Instant.now());

        StockExit saved = exitRepository.save(exit);
        auditService.log("StockExit", saved.getId(), AuditAction.MODIFICATION,
                "Sortie stock annulée " + saved.getExitNumber(), user);
        return mapper.toExitResponse(saved);
    }

    @Transactional
    public void deleteLine(Long exitId, Long lineId) {
        StockExit exit = findExit(exitId);
        ensureNotPosLinked(exit);
        ensureDraft(exit);
        boolean removed = exit.getLignes().removeIf(l -> l.getId().equals(lineId));
        if (!removed) {
            throw new ResourceNotFoundException("Ligne non trouvée: " + lineId);
        }
        exitRepository.save(exit);
    }

    @Transactional
    public void delete(Long id) {
        StockExit exit = findExit(id);
        ensureNotPosLinked(exit);
        ensureDraft(exit);
        exitRepository.delete(exit);
        auditService.log("StockExit", id, AuditAction.SUPPRESSION,
                "Sortie stock brouillon supprimée " + exit.getExitNumber(), exit.getCreatedBy());
    }

    private void postMovement(
            StockExit exit,
            StockExitLine line,
            String user,
            StockMovementType type,
            BigDecimal delta) {

        StockOperationRequest op = new StockOperationRequest();
        op.setProductId(line.getProduct().getId());
        op.setVariantId(line.getVariant() != null ? line.getVariant().getId() : null);
        op.setWarehouseId(exit.getWarehouse().getId());
        op.setLocationId(exit.getLocation().getId());
        op.setReferenceType("STOCK_EXIT");
        op.setReference(exit.getExitNumber());
        op.setReason(buildReasonLabel(exit, type));
        op.setUtilisateur(currentUserService.resolveActor(user));

        ledger.applyOnHandChange(
                op.getProductId(),
                op.getVariantId(),
                op.getWarehouseId(),
                op.getLocationId(),
                null,
                delta,
                StockService.meta(type, op, null, null, null, null, exit.getId()));
    }

    private String buildReasonLabel(StockExit exit, StockMovementType type) {
        String reasonLabel = exit.getReason().name();
        if (type == StockMovementType.OUT) {
            return "Sortie stock " + exit.getExitNumber() + " (" + reasonLabel + ")";
        }
        return "Annulation sortie " + exit.getExitNumber();
    }

    private void ensureAvailable(StockExit exit, StockExitLine line) {
        if (settingsService.getStockConfig().isAllowNegativeStock()) {
            return;
        }
        BigDecimal available = ledger.getAvailable(
                line.getProduct().getId(),
                line.getVariant() != null ? line.getVariant().getId() : null,
                exit.getWarehouse().getId());
        if (available.compareTo(line.getQuantityInBaseUnit()) < 0) {
            throw new BusinessException("Stock disponible insuffisant pour le produit "
                    + line.getProduct().getNom() + " (disponible: "
                    + available.stripTrailingZeros().toPlainString() + ")");
        }
    }

    private List<StockExitLine> buildLines(StockExit exit, List<StockExitRequest.Line> lineReqs) {
        List<StockExitLine> lines = new ArrayList<>();
        for (StockExitRequest.Line req : lineReqs) {
            Product product = loadProduct(req.getProductId());
            ProductVariant variant = resolveVariant(req.getProductId(), req.getVariantId());
            ProductPackaging packaging = loadPackagingOptional(req.getProductId(), req.getPackagingId());
            BigDecimal baseQty = resolveLineBaseQuantity(req);

            lines.add(StockExitLine.builder()
                    .stockExit(exit)
                    .product(product)
                    .variant(variant)
                    .packaging(packaging)
                    .quantityInput(req.getQuantityInput())
                    .quantityInBaseUnit(baseQty)
                    .notes(req.getNotes())
                    .build());
        }
        return lines;
    }

    private BigDecimal resolveLineBaseQuantity(StockExitRequest.Line line) {
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

    private String generateExitNumber() {
        String prefix = settingsService.getNumberingConfig().getExitPrefix()
                + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = exitRepository.countByExitNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private void ensureDraft(StockExit exit) {
        if (exit.getStatus() != StockExitStatus.DRAFT) {
            throw new BusinessException("Seule une sortie brouillon peut être modifiée");
        }
    }

    private void ensureNotPosLinked(StockExit exit) {
        if (exit.getSale() != null) {
            throw new BusinessException("Cette sortie provient du POS et ne peut pas être modifiée manuellement");
        }
    }

    private StockExit findExit(Long id) {
        return exitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sortie stock non trouvée: " + id));
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

    private ProductPackaging loadPackagingOptional(Long productId, Long packagingId) {
        if (packagingId == null) {
            return null;
        }
        return packagingRepository.findByIdAndProductId(packagingId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouvé"));
    }
}
