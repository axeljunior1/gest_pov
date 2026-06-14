package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.domain.enums.PurchaseOrderStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PurchaseOrderMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final SupplierPurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockEntryService stockEntryService;
    private final PurchaseOrderMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(PurchaseOrderStatus status, Long supplierId) {
        return orderRepository.findFiltered(status, supplierId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        return mapper.toResponse(findDetailed(id));
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur non trouvé"));

        SupplierPurchaseOrder order = SupplierPurchaseOrder.builder()
                .reference(generateReference())
                .supplier(supplier)
                .warehouse(loadWarehouseOptional(request.getWarehouseId()))
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(trimToNull(request.getNotes()))
                .status(PurchaseOrderStatus.PENDING)
                .lines(new ArrayList<>())
                .build();

        for (PurchaseOrderRequest.LineRequest lineReq : request.getLines()) {
            order.getLines().add(buildLine(order, lineReq));
        }

        SupplierPurchaseOrder saved = orderRepository.save(order);
        auditService.log("PurchaseOrder", saved.getId(), AuditAction.CREATION,
                "Commande fournisseur " + saved.getReference());
        return mapper.toResponse(findDetailed(saved.getId()));
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        SupplierPurchaseOrder order = findDetailed(id);
        if (order.getStatus() == PurchaseOrderStatus.DELIVERED) {
            throw new BusinessException("Impossible d'annuler une commande déjà livrée");
        }
        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException("Commande déjà annulée");
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        SupplierPurchaseOrder saved = orderRepository.save(order);
        auditService.log("PurchaseOrder", saved.getId(), AuditAction.MODIFICATION,
                "Commande annulée " + saved.getReference());
        return mapper.toResponse(findDetailed(saved.getId()));
    }

    @Transactional
    public PurchaseOrderResponse receive(Long id, PurchaseOrderReceiveRequest request) {
        SupplierPurchaseOrder order = findDetailed(id);
        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException("Commande annulée");
        }
        if (order.getStatus() == PurchaseOrderStatus.DELIVERED) {
            throw new BusinessException("Commande déjà entièrement reçue");
        }

        Map<Long, SupplierPurchaseOrderLine> lineById = order.getLines().stream()
                .collect(Collectors.toMap(SupplierPurchaseOrderLine::getId, l -> l));

        StockEntryRequest entryRequest = new StockEntryRequest();
        entryRequest.setSupplierId(order.getSupplier().getId());
        entryRequest.setWarehouseId(request.getWarehouseId());
        entryRequest.setLocationId(request.getLocationId());
        entryRequest.setEntryDate(LocalDate.now());
        entryRequest.setReferenceDocument(order.getReference());
        entryRequest.setNotes("Réception commande " + order.getReference());
        entryRequest.setCreatedBy(currentUserService.getCurrentUserEmailOrDefault());
        entryRequest.setLignes(new ArrayList<>());

        for (PurchaseOrderReceiveRequest.LineReceive recv : request.getLines()) {
            SupplierPurchaseOrderLine line = lineById.get(recv.getLineId());
            if (line == null) {
                throw new BusinessException("Ligne commande invalide: " + recv.getLineId());
            }
            BigDecimal remaining = line.getQuantity().subtract(
                    line.getReceivedQuantity() != null ? line.getReceivedQuantity() : BigDecimal.ZERO);
            if (recv.getQuantity().compareTo(remaining) > 0) {
                throw new BusinessException("Quantité reçue supérieure au reste à recevoir pour "
                        + line.getProduct().getNom());
            }

            StockEntryRequest.Line entryLine = new StockEntryRequest.Line();
            entryLine.setProductId(line.getProduct().getId());
            if (line.getVariant() != null) {
                entryLine.setVariantId(line.getVariant().getId());
            }
            entryLine.setQuantityInput(recv.getQuantity());
            entryLine.setUnitCost(line.getUnitPrice());
            entryRequest.getLignes().add(entryLine);

            line.setReceivedQuantity(
                    (line.getReceivedQuantity() != null ? line.getReceivedQuantity() : BigDecimal.ZERO)
                            .add(recv.getQuantity()));
        }

        StockEntryResponse entry = stockEntryService.create(entryRequest);
        if (request.isValidateEntry()) {
            entry = stockEntryService.validate(entry.getId(), currentUserService.getCurrentUserEmailOrDefault());
        }

        order.setWarehouse(warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé")));
        order.setStockEntry(stockEntryService.findEntity(entry.getId()));
        refreshOrderStatus(order);

        SupplierPurchaseOrder saved = orderRepository.save(order);
        auditService.log("PurchaseOrder", saved.getId(), AuditAction.MODIFICATION,
                "Réception commande " + saved.getReference() + " → entrée " + entry.getEntryNumber());
        return mapper.toResponse(findDetailed(saved.getId()));
    }

    SupplierPurchaseOrder findDetailed(Long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur non trouvée: " + id));
    }

    private SupplierPurchaseOrderLine buildLine(SupplierPurchaseOrder order, PurchaseOrderRequest.LineRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));
        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = variantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvée"));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BusinessException("Variante invalide pour le produit");
            }
        }
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantité commandée invalide");
        }
        return SupplierPurchaseOrderLine.builder()
                .purchaseOrder(order)
                .product(product)
                .variant(variant)
                .quantity(req.getQuantity())
                .receivedQuantity(BigDecimal.ZERO)
                .unitPrice(req.getUnitPrice())
                .notes(trimToNull(req.getNotes()))
                .build();
    }

    private void refreshOrderStatus(SupplierPurchaseOrder order) {
        boolean allReceived = order.getLines().stream().allMatch(l ->
                l.getReceivedQuantity() != null
                        && l.getReceivedQuantity().compareTo(l.getQuantity()) >= 0);
        boolean anyReceived = order.getLines().stream().anyMatch(l ->
                l.getReceivedQuantity() != null && l.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);

        if (allReceived) {
            order.setStatus(PurchaseOrderStatus.DELIVERED);
        } else if (anyReceived) {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
    }

    private Warehouse loadWarehouseOptional(Long id) {
        if (id == null) return null;
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepôt non trouvé"));
    }

    private String generateReference() {
        String prefix = "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long count = orderRepository.findAll().stream()
                .filter(o -> o.getReference() != null && o.getReference().startsWith(prefix))
                .count();
        return prefix + String.format("%03d", count + 1);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
