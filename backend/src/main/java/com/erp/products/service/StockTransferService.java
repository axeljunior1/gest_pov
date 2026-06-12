package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.domain.enums.StockTransferStatus;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.dto.StockTransferRequest;
import com.erp.products.dto.StockTransferResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
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
public class StockTransferService {

    private final StockTransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final StockLedgerService ledger;
    private final StockMapper mapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public StockTransferResponse create(StockTransferRequest request) {
        if (transferRepository.findByReference(request.getReference()).isPresent()) {
            throw new BusinessException("Reference transfert deja utilisee: " + request.getReference());
        }
        Warehouse source = loadWarehouse(request.getSourceWarehouseId());
        Warehouse dest = loadWarehouse(request.getDestWarehouseId());
        if (source.getId().equals(dest.getId())) {
            throw new BusinessException("Les entrepots source et destination doivent etre differents");
        }

        StockTransfer transfer = StockTransfer.builder()
                .reference(request.getReference())
                .sourceWarehouse(source)
                .destWarehouse(dest)
                .status(StockTransferStatus.DRAFT)
                .notes(request.getNotes())
                .utilisateur(currentUserService.resolveActor(request.getUtilisateur()))
                .lignes(new ArrayList<>())
                .build();

        for (StockTransferRequest.Line lineReq : request.getLignes()) {
            StockTransferLine line = StockTransferLine.builder()
                    .transfer(transfer)
                    .product(loadProduct(lineReq.getProductId()))
                    .variant(loadVariant(lineReq.getProductId(), lineReq.getVariantId()))
                    .lot(loadLot(lineReq.getLotId()))
                    .quantity(lineReq.getQuantity())
                    .sourceLocation(loadLocation(source.getId(), lineReq.getSourceLocationId()))
                    .destLocation(loadLocation(dest.getId(), lineReq.getDestLocationId()))
                    .build();
            transfer.getLignes().add(line);
        }

        return mapper.toTransferResponse(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse ship(Long transferId) {
        StockTransfer transfer = findTransfer(transferId);
        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new BusinessException("Seul un transfert brouillon peut etre expedie");
        }

        for (StockTransferLine line : transfer.getLignes()) {
            BigDecimal available = ledger.getAvailable(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    transfer.getSourceWarehouse().getId());
            if (available.compareTo(line.getQuantity()) < 0) {
                throw new BusinessException("Stock disponible insuffisant pour le transfert");
            }
            StockOperationRequest op = lineToOp(line, transfer, line.getSourceLocation().getWarehouse().getId(),
                    line.getSourceLocation().getId());
            ledger.applyOnHandChange(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    transfer.getSourceWarehouse().getId(),
                    line.getSourceLocation().getId(),
                    line.getLot() != null ? line.getLot().getId() : null,
                    line.getQuantity().negate(),
                    StockService.meta(StockMovementType.TRANSFER_OUT, op, transfer.getId(), null, null));
        }

        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        transfer.setShippedAt(Instant.now());
        return mapper.toTransferResponse(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferResponse receive(Long transferId) {
        StockTransfer transfer = findTransfer(transferId);
        if (transfer.getStatus() != StockTransferStatus.IN_TRANSIT) {
            throw new BusinessException("Seul un transfert en transit peut etre receptionne");
        }

        for (StockTransferLine line : transfer.getLignes()) {
            StockOperationRequest op = lineToOp(line, transfer, line.getDestLocation().getWarehouse().getId(),
                    line.getDestLocation().getId());
            ledger.applyOnHandChange(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    transfer.getDestWarehouse().getId(),
                    line.getDestLocation().getId(),
                    line.getLot() != null ? line.getLot().getId() : null,
                    line.getQuantity(),
                    StockService.meta(StockMovementType.TRANSFER_IN, op, transfer.getId(), null, null));
        }

        transfer.setStatus(StockTransferStatus.RECEIVED);
        transfer.setReceivedAt(Instant.now());
        return mapper.toTransferResponse(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<StockTransferResponse> findAll() {
        return transferRepository.findAll().stream()
                .map(mapper::toTransferResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getById(Long id) {
        return mapper.toTransferResponse(findTransfer(id));
    }

    private StockTransfer findTransfer(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfert non trouve: " + id));
    }

    private StockOperationRequest lineToOp(StockTransferLine line, StockTransfer transfer,
                                         Long warehouseId, Long locationId) {
        StockOperationRequest op = new StockOperationRequest();
        op.setProductId(line.getProduct().getId());
        op.setVariantId(line.getVariant() != null ? line.getVariant().getId() : null);
        op.setWarehouseId(warehouseId);
        op.setLocationId(locationId);
        op.setLotId(line.getLot() != null ? line.getLot().getId() : null);
        op.setQuantityBase(line.getQuantity());
        op.setReferenceType("TRANSFER");
        op.setReference(transfer.getReference());
        op.setUtilisateur(transfer.getUtilisateur());
        return op;
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
                .orElseThrow(() -> new ResourceNotFoundException("Variante non trouvee"));
        if (!v.getProduct().getId().equals(productId)) {
            throw new BusinessException("Variante invalide");
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
                .orElseThrow(() -> new ResourceNotFoundException("Lot non trouve"));
    }
}
