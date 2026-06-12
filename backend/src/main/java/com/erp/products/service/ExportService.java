package com.erp.products.service;

import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.ExportFormat;
import com.erp.products.domain.enums.InventoryCountStatus;
import com.erp.products.domain.enums.StockEntryStatus;
import com.erp.products.domain.enums.StockExitStatus;
import com.erp.products.dto.*;
import com.erp.products.mapper.AlertMapper;
import com.erp.products.mapper.ProductMapper;
import com.erp.products.repository.ProductRepository;
import com.erp.products.service.alert.AlertService;
import com.erp.products.specification.ProductSpecification;
import com.erp.products.util.TabularFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final StockService stockService;
    private final StockMovementService movementService;
    private final StockEntryService entryService;
    private final StockExitService exitService;
    private final AlertService alertService;
    private final AlertMapper alertMapper;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public byte[] exportProducts(ExportFormat format, ProductSearchCriteria criteria) {
        List<String> headers = List.of(
                "sku", "nom", "description", "marque", "categorieNom", "unitSymbole",
                "prixAchat", "prixVente", "statut", "cycleVie", "stockTotal");
        List<List<String>> rows = productRepository.findAll(ProductSpecification.fromCriteria(criteria)).stream()
                .map(p -> productMapper.toProductResponse(p, null, false))
                .map(p -> List.of(
                        str(p.getSku()), str(p.getNom()), str(p.getDescription()), str(p.getMarque()),
                        str(p.getCategorieNom()), str(p.getUnitSymbole()),
                        str(p.getPrixAchat()), str(p.getPrixVente()),
                        str(p.getStatut()), str(p.getCycleVie()), str(p.getStockTotal())))
                .toList();
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportStock(ExportFormat format, Long warehouseId, Long productId) {
        List<String> headers = List.of(
                "productId", "productNom", "variantId", "warehouseCode", "locationCode",
                "lotNumero", "quantityOnHand", "quantityReserved", "quantityAvailable", "unitSymbole");
        List<List<String>> rows = stockService.listItems(warehouseId, productId).stream()
                .map(i -> List.of(
                        str(i.getProductId()), str(i.getProductNom()), str(i.getVariantId()),
                        str(i.getWarehouseCode()), str(i.getLocationCode()), str(i.getLotNumero()),
                        str(i.getQuantityOnHand()), str(i.getQuantityReserved()),
                        str(i.getQuantityAvailable()), str(i.getUnitSymbole())))
                .toList();
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportMovements(ExportFormat format, StockMovementSearchCriteria criteria) {
        if (format == ExportFormat.CSV && criteria.getLimit() == null) {
            criteria.setLimit(5000);
        } else if (criteria.getLimit() == null) {
            criteria.setLimit(5000);
        }
        List<String> headers = List.of(
                "id", "date", "type", "productNom", "warehouseCode", "locationCode",
                "quantity", "unitSymbole", "quantityBefore", "quantityAfter",
                "referenceType", "referenceId", "reference", "reason", "utilisateur");
        List<List<String>> rows = movementService.listMovements(criteria).stream()
                .map(m -> List.of(
                        str(m.getId()), str(m.getMovementDate()), str(m.getMovementType()),
                        str(m.getProductNom()), str(m.getWarehouseCode()), str(m.getLocationCode()),
                        str(m.getQuantity()), str(m.getUnitSymbole()),
                        str(m.getQuantityBefore()), str(m.getQuantityAfter()),
                        str(m.getReferenceType()), str(m.getReferenceId()), str(m.getReference()),
                        str(m.getReason()), str(m.getCreatedBy())))
                .toList();
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportEntries(ExportFormat format, Long productId, Long warehouseId,
                                StockEntryStatus status, LocalDate dateFrom, LocalDate dateTo) {
        List<String> headers = List.of(
                "entryNumber", "status", "warehouseCode", "locationCode", "entryDate",
                "referenceDocument", "productSku", "productNom", "quantityInBaseUnit", "validatedAt");
        List<List<String>> rows = new ArrayList<>();
        for (StockEntryResponse entry : entryService.findAll(productId, null, warehouseId, status, dateFrom, dateTo)) {
            if (entry.getLignes() == null || entry.getLignes().isEmpty()) {
                rows.add(List.of(
                        str(entry.getEntryNumber()), str(entry.getStatus()), str(entry.getWarehouseCode()),
                        str(entry.getLocationCode()), str(entry.getEntryDate()), str(entry.getReferenceDocument()),
                        "", "", "", str(entry.getValidatedAt())));
            } else {
                for (StockEntryResponse.Line line : entry.getLignes()) {
                    rows.add(List.of(
                            str(entry.getEntryNumber()), str(entry.getStatus()), str(entry.getWarehouseCode()),
                            str(entry.getLocationCode()), str(entry.getEntryDate()), str(entry.getReferenceDocument()),
                            str(line.getProductId()), str(line.getProductNom()),
                            str(line.getQuantityInBaseUnit()), str(entry.getValidatedAt())));
                }
            }
        }
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportExits(ExportFormat format, Long productId, Long warehouseId,
                              StockExitStatus status, LocalDate dateFrom, LocalDate dateTo) {
        List<String> headers = List.of(
                "exitNumber", "status", "reason", "warehouseCode", "locationCode", "exitDate",
                "productNom", "quantityInBaseUnit", "validatedAt");
        List<List<String>> rows = new ArrayList<>();
        for (StockExitResponse exit : exitService.findAll(productId, warehouseId, status, null, dateFrom, dateTo)) {
            if (exit.getLignes() == null || exit.getLignes().isEmpty()) {
                rows.add(List.of(
                        str(exit.getExitNumber()), str(exit.getStatus()), str(exit.getReason()),
                        str(exit.getWarehouseCode()), str(exit.getLocationCode()), str(exit.getExitDate()),
                        "", "", str(exit.getValidatedAt())));
            } else {
                for (StockExitResponse.Line line : exit.getLignes()) {
                    rows.add(List.of(
                            str(exit.getExitNumber()), str(exit.getStatus()), str(exit.getReason()),
                            str(exit.getWarehouseCode()), str(exit.getLocationCode()), str(exit.getExitDate()),
                            str(line.getProductNom()), str(line.getQuantityInBaseUnit()), str(exit.getValidatedAt())));
                }
            }
        }
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportAlerts(ExportFormat format, AlertType type, AlertStatus status,
                               Long productId, Long warehouseId) {
        List<String> headers = List.of(
                "type", "severity", "status", "productNom", "warehouseCode", "locationCode",
                "lotNumero", "message", "triggeredValue", "thresholdValue", "lastTriggeredAt");
        List<List<String>> rows = alertService.findFiltered(type, status, productId, warehouseId).stream()
                .map(alertMapper::toAlertResponse)
                .map(a -> List.of(
                        str(a.getType()), str(a.getSeverity()), str(a.getStatus()), str(a.getProductNom()),
                        str(a.getWarehouseCode()), str(a.getLocationCode()), str(a.getLotNumero()),
                        str(a.getMessage()), str(a.getTriggeredValue()), str(a.getThresholdValue()),
                        str(a.getLastTriggeredAt())))
                .toList();
        return TabularFileHelper.write(format, headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportInventories(ExportFormat format, Long warehouseId, InventoryCountStatus status) {
        List<String> headers = List.of(
                "inventoryNumber", "status", "warehouseCode", "locationCode",
                "productNom", "quantitySystem", "quantityCounted", "ecart", "validatedAt");
        List<List<String>> rows = new ArrayList<>();
        for (InventoryCountResponse inv : inventoryService.findAll(warehouseId, null, status, null, null)) {
            if (inv.getLignes() == null || inv.getLignes().isEmpty()) {
                rows.add(List.of(
                        str(inv.getInventoryNumber()), str(inv.getStatus()), str(inv.getWarehouseCode()),
                        str(inv.getLocationCode()), "", "", "", "", str(inv.getValidatedAt())));
            } else {
                for (InventoryCountResponse.Line line : inv.getLignes()) {
                    rows.add(List.of(
                            str(inv.getInventoryNumber()), str(inv.getStatus()), str(inv.getWarehouseCode()),
                            str(inv.getLocationCode()), str(line.getProductNom()),
                            str(line.getQuantitySystem()), str(line.getQuantityCounted()),
                            str(line.getDifferenceQuantity()), str(inv.getValidatedAt())));
                }
            }
        }
        return TabularFileHelper.write(format, headers, rows);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
