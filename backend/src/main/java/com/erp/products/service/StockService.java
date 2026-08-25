package com.erp.products.service;

import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.StockMovement;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockAvailableResponse;
import com.erp.products.dto.StockItemResponse;
import com.erp.products.dto.StockMovementResponse;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.domain.entity.User;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.StockItemRepository;
import com.erp.products.repository.UserRepository;
import com.erp.products.security.CurrentUserService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockLedgerService ledger;
    private final StockItemRepository stockItemRepository;
    private final ProductRepository productRepository;
    private final ProductPackagingRepository packagingRepository;
    private final StockMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BadgePinAuthService badgePinAuthService;
    private final com.erp.products.service.stockvaluation.StockCmpValuationService cmpValuationService;

    @Transactional
    public StockMovementResponse receive(StockOperationRequest request) {
        BigDecimal qty = resolveBaseQuantity(request);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantite de reception doit etre positive");
        }
        String actor = currentUserService.resolveActor(request.getUtilisateur());
        StockMovement movement = ledger.applyOnHandChange(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getLotId(),
                qty,
                meta(StockMovementType.IN, request, null, null, null, null, null, actor));
        auditService.log("Stock", request.getProductId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Réception stock: +" + qty.stripTrailingZeros().toPlainString(), actor);
        cmpValuationService.recordPurchase(
                request.getProductId(),
                request.getVariantId(),
                qty,
                cmpValuationService.resolveFallbackCost(request.getProductId(), request.getVariantId()),
                Instant.now(),
                movement.getId(),
                "STOCK_RECEIPT");
        return mapper.toMovementResponse(movement);
    }

    @Transactional
    public StockMovementResponse issue(StockOperationRequest request) {
        BigDecimal qty = resolveBaseQuantity(request);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantite de sortie doit etre positive");
        }
        ensureAvailable(request, qty);
        String actor = currentUserService.resolveActor(request.getUtilisateur());
        StockMovement movement = ledger.applyOnHandChange(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getLotId(),
                qty.negate(),
                meta(StockMovementType.OUT, request, null, null, null, null, null, actor));
        auditService.log("Stock", request.getProductId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Sortie stock: -" + qty.stripTrailingZeros().toPlainString(), actor);
        cmpValuationService.recordSale(
                request.getProductId(),
                request.getVariantId(),
                qty,
                Instant.now(),
                movement.getId(),
                "STOCK_ISSUE");
        return mapper.toMovementResponse(movement);
    }

    @Transactional
    public StockMovementResponse adjust(StockOperationRequest request) {
        if (request.getQuantityBase() == null) {
            throw new BusinessException("Un ajustement requiert quantityBase (positif ou négatif)");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException("Un motif est obligatoire pour tout ajustement de stock");
        }
        BigDecimal delta = request.getQuantityBase();
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            ensureAvailable(request, delta.abs());
        }
        validateManagerIfRequired(delta.abs(), request);
        String actor = currentUserService.resolveActor(request.getUtilisateur());
        StockMovement movement = ledger.applyOnHandChange(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getLotId(),
                delta,
                meta(StockMovementType.ADJUSTMENT, request, null, null, null, null, null, actor));
        auditService.log("Stock", request.getProductId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Ajustement stock: " + delta.stripTrailingZeros().toPlainString()
                        + " — motif: " + request.getReason().trim(), actor);
        cmpValuationService.recordInventoryAdjustment(
                request.getProductId(),
                request.getVariantId(),
                delta,
                Instant.now(),
                movement.getId(),
                "STOCK_ADJUSTMENT");
        return mapper.toMovementResponse(movement);
    }

    /** Contrôle à deux : au-delà du seuil configuré, un second utilisateur (avec droit stock.adjust) doit se ré-authentifier. */
    private void validateManagerIfRequired(BigDecimal quantityDelta, StockOperationRequest request) {
        BigDecimal threshold = settingsService.getDecimal(SettingKeys.STOCK_REQUIRE_MANAGER_APPROVAL_ABOVE_ADJUSTMENT_AMOUNT);
        if (threshold == null || quantityDelta.compareTo(threshold) <= 0) {
            return;
        }
        boolean hasBadge = request.getManagerBadgeCode() != null && !request.getManagerBadgeCode().isBlank()
                && request.getManagerPin() != null && !request.getManagerPin().isBlank();
        boolean hasEmailPwd = request.getManagerEmail() != null && !request.getManagerEmail().isBlank()
                && request.getManagerPassword() != null && !request.getManagerPassword().isBlank();
        if (!hasBadge && !hasEmailPwd) {
            throw new BusinessException("Validation manager obligatoire pour cet ajustement de stock");
        }
        User manager;
        if (hasBadge) {
            manager = badgePinAuthService.authenticate(request.getManagerBadgeCode(), request.getManagerPin());
        } else {
            manager = userRepository.findByEmailIgnoreCase(request.getManagerEmail().trim())
                    .orElseThrow(() -> new BusinessException("Manager introuvable"));
            if (!passwordEncoder.matches(request.getManagerPassword(), manager.getPasswordHash())) {
                throw new BusinessException("Identifiants manager invalides");
            }
        }
        User actor = currentUserService.requireCurrentUser();
        if (manager.getEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new BusinessException("La validation manager doit etre effectuee par un autre utilisateur");
        }
        boolean authorized = manager.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> "stock.adjust".equals(p.getCode()));
        if (!authorized) {
            throw new BusinessException("Cet utilisateur ne peut pas valider cet ajustement");
        }
    }

    @Transactional
    public StockMovementResponse applyInitialStock(StockOperationRequest request, String importReference) {
        if (request.getQuantityBase() == null || request.getQuantityBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La quantite de stock initial doit etre positive");
        }
        String actor = currentUserService.resolveActor(request.getUtilisateur());
        request.setReferenceType("INITIAL_STOCK");
        request.setReference(importReference != null ? importReference : "IMPORT");
        StockMovement movement = ledger.applyOnHandChange(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getLotId(),
                request.getQuantityBase(),
                new StockLedgerService.MovementMeta(
                        StockMovementType.INITIAL_STOCK,
                        "INITIAL_STOCK",
                        request.getReference(),
                        "Stock initial importe",
                        actor,
                        null, null, null, null, null));
        auditService.log("Stock", request.getProductId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Stock initial importe: +" + request.getQuantityBase().stripTrailingZeros().toPlainString(), actor);
        cmpValuationService.recordPurchase(
                request.getProductId(),
                request.getVariantId(),
                request.getQuantityBase(),
                cmpValuationService.resolveFallbackCost(request.getProductId(), request.getVariantId()),
                Instant.now(),
                movement.getId(),
                "INITIAL_STOCK");
        return mapper.toMovementResponse(movement);
    }

    @Transactional(readOnly = true)
    public StockAvailableResponse getAvailable(Long productId, Long variantId, Long warehouseId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + productId));
        BigDecimal onHand = stockItemRepository.findByProductId(productId).stream()
                .filter(i -> warehouseId == null || i.getWarehouse().getId().equals(warehouseId))
                .filter(i -> variantId == null
                        ? i.getVariant() == null
                        : i.getVariant() != null && i.getVariant().getId().equals(variantId))
                .map(i -> i.getQuantityOnHand())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reserved = stockItemRepository.findByProductId(productId).stream()
                .filter(i -> warehouseId == null || i.getWarehouse().getId().equals(warehouseId))
                .filter(i -> variantId == null
                        ? i.getVariant() == null
                        : i.getVariant() != null && i.getVariant().getId().equals(variantId))
                .map(i -> i.getQuantityReserved())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = ledger.getAvailable(productId, variantId, warehouseId);
        return StockAvailableResponse.builder()
                .productId(productId)
                .variantId(variantId)
                .warehouseId(warehouseId)
                .unitSymbole(product.getUnit() != null ? product.getUnit().getSymbole() : null)
                .quantityOnHand(onHand)
                .quantityReserved(reserved)
                .quantityAvailable(available)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockItemResponse> listItems(Long warehouseId, Long productId) {
        List<com.erp.products.domain.entity.StockItem> items;
        if (warehouseId != null) {
            items = stockItemRepository.findByWarehouseId(warehouseId);
        } else if (productId != null) {
            items = stockItemRepository.findByProductId(productId);
        } else {
            items = stockItemRepository.findAll();
        }
        return items.stream().map(mapper::toStockItemResponse).collect(Collectors.toList());
    }

    BigDecimal resolveBaseQuantity(StockOperationRequest request) {
        if (request.getQuantityBase() != null) {
            return request.getQuantityBase().setScale(6, RoundingMode.HALF_UP);
        }
        if (request.getPackagingId() != null && request.getPackagingQuantity() != null) {
            ProductPackaging packaging = packagingRepository.findByIdAndProductId(
                            request.getPackagingId(), request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conditionnement non trouvé"));
            return request.getPackagingQuantity()
                    .multiply(packaging.getQuantiteBase())
                    .setScale(6, RoundingMode.HALF_UP);
        }
        throw new BusinessException("Indiquez quantityBase ou packagingId + packagingQuantity");
    }

    private void ensureAvailable(StockOperationRequest request, BigDecimal qty) {
        if (settingsService.getStockConfig().isAllowNegativeStock()) {
            return;
        }
        BigDecimal available = ledger.getAvailable(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId());
        if (available.compareTo(qty) < 0) {
            throw new BusinessException("Stock disponible insuffisant (disponible: "
                    + available.stripTrailingZeros().toPlainString() + ")");
        }
    }

    static StockLedgerService.MovementMeta meta(
            StockMovementType type,
            StockOperationRequest req,
            Long transferId,
            Long reservationId,
            Long inventoryId) {
        return meta(type, req, transferId, reservationId, inventoryId, null, null, req.getUtilisateur());
    }

    static StockLedgerService.MovementMeta meta(
            StockMovementType type,
            StockOperationRequest req,
            Long transferId,
            Long reservationId,
            Long inventoryId,
            Long stockEntryId) {
        return meta(type, req, transferId, reservationId, inventoryId, stockEntryId, null, req.getUtilisateur());
    }

    static StockLedgerService.MovementMeta meta(
            StockMovementType type,
            StockOperationRequest req,
            Long transferId,
            Long reservationId,
            Long inventoryId,
            Long stockEntryId,
            Long stockExitId) {
        return meta(type, req, transferId, reservationId, inventoryId, stockEntryId, stockExitId, req.getUtilisateur());
    }

    static StockLedgerService.MovementMeta meta(
            StockMovementType type,
            StockOperationRequest req,
            Long transferId,
            Long reservationId,
            Long inventoryId,
            Long stockEntryId,
            Long stockExitId,
            String utilisateur) {
        return new StockLedgerService.MovementMeta(
                type,
                req.getReferenceType(),
                req.getReference(),
                req.getReason(),
                utilisateur,
                transferId,
                reservationId,
                inventoryId,
                stockEntryId,
                stockExitId);
    }
}
