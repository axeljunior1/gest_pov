package com.erp.products.service;

import com.erp.products.domain.entity.ProductPackaging;
import com.erp.products.domain.entity.StockMovement;
import com.erp.products.domain.enums.StockMovementType;
import com.erp.products.dto.StockMovementCreateCommand;
import com.erp.products.dto.StockMovementResponse;
import com.erp.products.dto.StockMovementSearchCriteria;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.ProductPackagingRepository;
import com.erp.products.repository.StockMovementRepository;
import com.erp.products.specification.StockMovementSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductPackagingRepository packagingRepository;
    private final StockMapper mapper;

    /**
     * Point d'enregistrement unique — appelé uniquement par {@link StockLedgerService}.
     */
    @Transactional
    public StockMovement createMovement(StockMovementCreateCommand command) {
        ProductPackaging packaging = null;
        if (command.packagingId() != null) {
            packaging = packagingRepository.findById(command.packagingId()).orElse(null);
        }

        Long referenceId = resolveReferenceId(command);
        String actor = command.createdBy() != null ? command.createdBy() : "system";

        StockMovement movement = StockMovement.builder()
                .movementType(command.movementType())
                .product(command.product())
                .variant(command.variant())
                .warehouse(command.warehouse())
                .location(command.location())
                .lot(command.lot())
                .unit(command.unit())
                .packaging(packaging)
                .quantity(command.quantity())
                .quantityOnHandBefore(command.quantityOnHandBefore())
                .quantityOnHandAfter(command.quantityOnHandAfter())
                .quantityReservedBefore(command.quantityReservedBefore())
                .quantityReservedAfter(command.quantityReservedAfter())
                .referenceType(command.referenceType())
                .referenceId(referenceId)
                .reference(command.reference())
                .reason(command.reason())
                .notes(command.notes())
                .utilisateur(actor)
                .movementDate(Instant.now())
                .stockTransferId(command.stockTransferId())
                .stockReservationId(command.stockReservationId())
                .inventoryCountId(command.inventoryCountId())
                .stockEntryId(command.stockEntryId())
                .stockExitId(command.stockExitId())
                .build();

        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> listMovements(StockMovementSearchCriteria criteria) {
        int limit = criteria.getLimit() != null && criteria.getLimit() > 0
                ? Math.min(criteria.getLimit(), 2000)
                : 500;
        return movementRepository.findAll(
                        StockMovementSpecification.fromCriteria(criteria),
                        PageRequest.of(0, limit))
                .stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockMovementResponse getMovementById(Long id) {
        return mapper.toMovementResponse(findMovement(id));
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByProduct(Long productId) {
        StockMovementSearchCriteria criteria = new StockMovementSearchCriteria();
        criteria.setProductId(productId);
        return listMovements(criteria);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByWarehouse(Long warehouseId) {
        StockMovementSearchCriteria criteria = new StockMovementSearchCriteria();
        criteria.setWarehouseId(warehouseId);
        return listMovements(criteria);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByReference(String referenceType, Long referenceId) {
        return movementRepository
                .findByReferenceTypeAndReferenceIdOrderByMovementDateDescCreatedAtDesc(referenceType, referenceId)
                .stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] exportMovements(StockMovementSearchCriteria criteria) {
        criteria.setLimit(5000);
        List<StockMovementResponse> rows = listMovements(criteria);
        StringBuilder csv = new StringBuilder();
        csv.append("id;date;type;produit;entrepot;emplacement;quantite;unite;avant;apres;reference_type;reference_id;reference;motif;utilisateur\n");
        for (StockMovementResponse row : rows) {
            csv.append(row.getId()).append(';')
                    .append(row.getMovementDate()).append(';')
                    .append(row.getMovementType()).append(';')
                    .append(escape(row.getProductNom())).append(';')
                    .append(escape(row.getWarehouseCode())).append(';')
                    .append(escape(row.getLocationCode())).append(';')
                    .append(formatQty(row.getQuantity())).append(';')
                    .append(escape(row.getUnitSymbole())).append(';')
                    .append(formatQty(row.getQuantityBefore())).append(';')
                    .append(formatQty(row.getQuantityAfter())).append(';')
                    .append(escape(row.getReferenceType())).append(';')
                    .append(row.getReferenceId() != null ? row.getReferenceId() : "").append(';')
                    .append(escape(row.getReference())).append(';')
                    .append(escape(row.getReason())).append(';')
                    .append(escape(row.getCreatedBy())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findRecent() {
        return movementRepository.findTop200ByOrderByMovementDateDescCreatedAtDesc().stream()
                .map(mapper::toMovementResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findByType(StockMovementType type) {
        StockMovementSearchCriteria criteria = new StockMovementSearchCriteria();
        criteria.setMovementType(type);
        return listMovements(criteria);
    }

    private StockMovement findMovement(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement de stock non trouvé: " + id));
    }

    private Long resolveReferenceId(StockMovementCreateCommand command) {
        if (command.referenceId() != null) {
            return command.referenceId();
        }
        if (command.stockEntryId() != null) {
            return command.stockEntryId();
        }
        if (command.stockExitId() != null) {
            return command.stockExitId();
        }
        if (command.inventoryCountId() != null) {
            return command.inventoryCountId();
        }
        if (command.stockTransferId() != null) {
            return command.stockTransferId();
        }
        if (command.stockReservationId() != null) {
            return command.stockReservationId();
        }
        return null;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\n", " ");
    }

    private static String formatQty(java.math.BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
