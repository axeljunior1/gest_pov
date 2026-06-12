package com.erp.products.service;

import com.erp.products.domain.entity.StockEntry;
import com.erp.products.domain.entity.StockExit;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import com.erp.products.domain.enums.StockEntryStatus;
import com.erp.products.domain.enums.StockExitStatus;
import com.erp.products.dto.*;
import com.erp.products.mapper.StockMapper;
import com.erp.products.repository.*;
import com.erp.products.service.alert.AlertSettingResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final ProductRepository productRepository;
    private final StockItemRepository stockItemRepository;
    private final AlertRepository alertRepository;
    private final StockMovementRepository movementRepository;
    private final StockEntryRepository entryRepository;
    private final StockExitRepository exitRepository;
    private final StockMapper stockMapper;
    private final AlertSettingResolver alertSettingResolver;

    @Transactional(readOnly = true)
    public DashboardStockSummaryResponse getStockSummary() {
        long totalProducts = productRepository.count();
        BigDecimal totalQuantity = stockItemRepository.sumTotalQuantityOnHand();
        BigDecimal stockValue = getStockValue();

        var settings = alertSettingResolver.resolve(null, null);
        BigDecimal minLevel = settings.minStockLevel();

        long outOfStock = 0;
        long lowStock = 0;
        for (Object[] row : stockItemRepository.sumAvailableStockPerProduct()) {
            BigDecimal available = (BigDecimal) row[1];
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStock++;
            } else if (minLevel != null && available.compareTo(minLevel) <= 0) {
                lowStock++;
            }
        }

        return DashboardStockSummaryResponse.builder()
                .totalProducts(totalProducts)
                .totalStockQuantity(totalQuantity)
                .stockValue(stockValue)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getStockValue() {
        return stockItemRepository.sumStockValue();
    }

    @Transactional(readOnly = true)
    public DashboardAlertSummaryResponse getAlertSummary() {
        return DashboardAlertSummaryResponse.builder()
                .openAlerts(alertRepository.countByStatus(AlertStatus.OPEN))
                .openLowStock(alertRepository.countByStatusAndType(AlertStatus.OPEN, AlertType.LOW_STOCK))
                .openOutOfStock(alertRepository.countByStatusAndType(AlertStatus.OPEN, AlertType.OUT_OF_STOCK))
                .openExpirySoon(alertRepository.countByStatusAndType(AlertStatus.OPEN, AlertType.EXPIRY_SOON))
                .openExpired(alertRepository.countByStatusAndType(AlertStatus.OPEN, AlertType.EXPIRED))
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getRecentMovements(Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        return movementRepository
                .findTop200ByOrderByMovementDateDescCreatedAtDesc()
                .stream()
                .limit(effectiveLimit)
                .map(stockMapper::toMovementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopMovedProductResponse> getTopMovedProducts(Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        return movementRepository.findTopMovedProducts(PageRequest.of(0, effectiveLimit))
                .stream()
                .map(row -> TopMovedProductResponse.builder()
                        .productId((Long) row[0])
                        .productNom((String) row[1])
                        .movementCount((Long) row[2])
                        .totalQuantityMoved((BigDecimal) row[3])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseStockSummaryResponse> getWarehouseSummary() {
        return stockItemRepository.summarizeByWarehouse().stream()
                .map(row -> WarehouseStockSummaryResponse.builder()
                        .warehouseId((Long) row[0])
                        .warehouseCode((String) row[1])
                        .warehouseNom((String) row[2])
                        .totalQuantity((BigDecimal) row[3])
                        .stockValue((BigDecimal) row[4])
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardRecentEntryResponse> getRecentEntries(Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        return entryRepository.findTop10ByStatusOrderByValidatedAtDescCreatedAtDesc(StockEntryStatus.VALIDATED)
                .stream()
                .limit(effectiveLimit)
                .map(this::toRecentEntry)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardRecentExitResponse> getRecentExits(Integer limit) {
        int effectiveLimit = resolveLimit(limit);
        return exitRepository.findTop10ByStatusOrderByValidatedAtDescCreatedAtDesc(StockExitStatus.VALIDATED)
                .stream()
                .limit(effectiveLimit)
                .map(this::toRecentExit)
                .toList();
    }

    private DashboardRecentEntryResponse toRecentEntry(StockEntry entry) {
        return DashboardRecentEntryResponse.builder()
                .id(entry.getId())
                .entryNumber(entry.getEntryNumber())
                .warehouseCode(entry.getWarehouse().getCode())
                .status(entry.getStatus())
                .entryDate(entry.getEntryDate())
                .validatedAt(entry.getValidatedAt())
                .lineCount(entry.getLignes().size())
                .build();
    }

    private DashboardRecentExitResponse toRecentExit(StockExit exit) {
        return DashboardRecentExitResponse.builder()
                .id(exit.getId())
                .exitNumber(exit.getExitNumber())
                .warehouseCode(exit.getWarehouse().getCode())
                .status(exit.getStatus())
                .reason(exit.getReason())
                .exitDate(exit.getExitDate())
                .validatedAt(exit.getValidatedAt())
                .lineCount(exit.getLignes().size())
                .build();
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
