package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.domain.enums.StockReservationStatus;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.dto.StockReservationRequest;
import com.erp.products.dto.StockReservationResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final StockLedgerService ledger;
    private final StockMapper mapper;

    @Transactional
    public StockReservationResponse reserve(StockReservationRequest request) {
        StockReservation reservation = reservationRepository.save(
                StockReservation.builder()
                        .product(loadProduct(request.getProductId()))
                        .variant(loadVariant(request.getProductId(), request.getVariantId()))
                        .warehouse(loadWarehouse(request.getWarehouseId()))
                        .location(loadLocation(request.getWarehouseId(), request.getLocationId()))
                        .lot(loadLot(request.getLotId()))
                        .quantity(request.getQuantity())
                        .reference(request.getReference())
                        .utilisateur(request.getUtilisateur())
                        .status(StockReservationStatus.ACTIVE)
                        .build());

        ledger.applyReservedChange(
                request.getProductId(),
                request.getVariantId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getLotId(),
                request.getQuantity(),
                new StockLedgerService.MovementMeta(
                        StockMovementType.RESERVATION,
                        "RESERVATION",
                        request.getReference(),
                        "Reservation stock",
                        request.getUtilisateur(),
                        null,
                        reservation.getId(),
                        null,
                        null));

        return mapper.toReservationResponse(reservation);
    }

    @Transactional
    public StockReservationResponse release(Long reservationId) {
        StockReservation reservation = findActive(reservationId);
        ledger.applyReservedChange(
                reservation.getProduct().getId(),
                reservation.getVariant() != null ? reservation.getVariant().getId() : null,
                reservation.getWarehouse().getId(),
                reservation.getLocation().getId(),
                reservation.getLot() != null ? reservation.getLot().getId() : null,
                reservation.getQuantity().negate(),
                new StockLedgerService.MovementMeta(
                        StockMovementType.RELEASE,
                        "RESERVATION",
                        reservation.getReference(),
                        "Liberation reservation",
                        reservation.getUtilisateur(),
                        null,
                        reservation.getId(),
                        null,
                        null));

        reservation.setStatus(StockReservationStatus.RELEASED);
        return mapper.toReservationResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public StockReservationResponse consume(Long reservationId) {
        StockReservation reservation = findActive(reservationId);
        StockOperationRequest op = new StockOperationRequest();
        op.setProductId(reservation.getProduct().getId());
        op.setVariantId(reservation.getVariant() != null ? reservation.getVariant().getId() : null);
        op.setWarehouseId(reservation.getWarehouse().getId());
        op.setLocationId(reservation.getLocation().getId());
        op.setLotId(reservation.getLot() != null ? reservation.getLot().getId() : null);
        op.setQuantityBase(reservation.getQuantity());
        op.setReference(reservation.getReference());
        op.setUtilisateur(reservation.getUtilisateur());

        ledger.applyReservedChange(
                op.getProductId(), op.getVariantId(), op.getWarehouseId(), op.getLocationId(), op.getLotId(),
                reservation.getQuantity().negate(),
                new StockLedgerService.MovementMeta(
                        StockMovementType.RELEASE, "RESERVATION", op.getReference(),
                        "Consommation reservation", op.getUtilisateur(), null, reservation.getId(), null, null));

        ledger.applyOnHandChange(
                op.getProductId(), op.getVariantId(), op.getWarehouseId(), op.getLocationId(), op.getLotId(),
                reservation.getQuantity().negate(),
                StockService.meta(StockMovementType.OUT, op, null, reservation.getId(), null));

        reservation.setStatus(StockReservationStatus.CONSUMED);
        return mapper.toReservationResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<StockReservationResponse> findActive() {
        return reservationRepository.findByStatusOrderByCreatedAtDesc(StockReservationStatus.ACTIVE).stream()
                .map(mapper::toReservationResponse)
                .collect(Collectors.toList());
    }

    private StockReservation findActive(Long id) {
        StockReservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation non trouvee: " + id));
        if (r.getStatus() != StockReservationStatus.ACTIVE) {
            throw new BusinessException("Reservation non active");
        }
        return r;
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouve: " + id));
    }

    private ProductVariant loadVariant(Long productId, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvee: " + variantId));
        if (!v.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide pour ce produit");
        }
        return v;
    }

    private Warehouse loadWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot non trouve: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException("Lot non trouve: " + lotId));
    }
}
