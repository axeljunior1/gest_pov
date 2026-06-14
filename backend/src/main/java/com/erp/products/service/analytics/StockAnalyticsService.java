package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.*;
import com.erp.products.repository.AnalyticsRepository;
import com.erp.products.repository.StockItemRepository;
import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;
    private final StockItemRepository stockItemRepository;
    private final SettingsService settingsService;

    public AnalyticsStockAlertsResponse getStockAlerts(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        BigDecimal threshold = settingsService.getStockConfig().getLowStockThresholdDefault();
        if (threshold == null) {
            threshold = BigDecimal.TEN;
        }

        Map<Long, BigDecimal> soldQty = new HashMap<>();
        for (Object[] row : analyticsRepository.findProductSalesQuantities(filter.getFrom(), filter.getTo(), filter)) {
            soldQty.put(((Number) row[0]).longValue(), AnalyticsConstants.decimalValue(row[1]));
        }

        List<Object[]> stockRows = stockItemRepository.findProductAvailableQuantities();
        List<AnalyticsStockAlertRow> items = new ArrayList<>();

        for (Object[] row : stockRows) {
            Long productId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String sku = (String) row[2];
            BigDecimal available = AnalyticsConstants.decimalValue(row[3]);
            BigDecimal sold = soldQty.getOrDefault(productId, BigDecimal.ZERO);

            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                items.add(alert(productId, name, sku, available, sold, "OUT_OF_STOCK", "critical"));
            } else if (available.compareTo(threshold) <= 0) {
                items.add(alert(productId, name, sku, available, sold, "LOW_STOCK", "warning"));
            } else if (sold.compareTo(BigDecimal.ZERO) > 0 && available.compareTo(sold) < 0) {
                items.add(alert(productId, name, sku, available, sold, "REORDER", "info"));
            }
        }

        items.sort((a, b) -> severityRank(a.getSeverity()) - severityRank(b.getSeverity()));
        if (items.size() > 50) {
            items = items.subList(0, 50);
        }

        return AnalyticsStockAlertsResponse.builder().items(items).build();
    }

    public AnalyticsBusinessAlertsResponse getBusinessAlerts(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        List<AnalyticsBusinessAlertRow> items = new ArrayList<>();

        for (Object[] row : analyticsRepository.findNeverSoldProducts()) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("DORMANT_PRODUCT")
                    .severity("info")
                    .title("Produit jamais vendu")
                    .message((String) row[1])
                    .entityType("PRODUCT")
                    .entityId(((Number) row[0]).longValue())
                    .build());
        }

        for (Object[] row : analyticsRepository.findHighDiscountSales(
                filter.getFrom(), filter.getTo(), BigDecimal.valueOf(50))) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("HIGH_DISCOUNT")
                    .severity("warning")
                    .title("Remise élevée")
                    .message("Vente " + row[1])
                    .value(AnalyticsConstants.decimalValue(row[2]))
                    .entityType("SALE")
                    .entityId(((Number) row[0]).longValue())
                    .build());
        }

        long cancellations = analyticsRepository.countCancelledSales(filter.getFrom(), filter.getTo(), filter);
        if (cancellations >= 5) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("HIGH_CANCELLATIONS")
                    .severity("warning")
                    .title("Annulations élevées")
                    .message(cancellations + " ventes annulées sur la période")
                    .value(BigDecimal.valueOf(cancellations))
                    .build());
        }

        var today = filterService.dayRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")));
        long todayCancellations = analyticsRepository.countCancelledSales(today.from(), today.to(), filter);
        if (todayCancellations >= 10) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("HIGH_CANCELLATIONS_TODAY")
                    .severity("critical")
                    .title("Trop d'annulations aujourd'hui")
                    .message(todayCancellations + " ventes annulées aujourd'hui")
                    .value(BigDecimal.valueOf(todayCancellations))
                    .build());
        }

        BigDecimal cancelledAmount = analyticsRepository.sumCancelledAmount(filter.getFrom(), filter.getTo(), filter);
        if (cancelledAmount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("HIGH_CANCELLED_AMOUNT")
                    .severity("warning")
                    .title("Montant annulé élevé")
                    .message("Montant total annulé sur la période : " + cancelledAmount.stripTrailingZeros().toPlainString())
                    .value(cancelledAmount)
                    .build());
        }

        var topCancellers = analyticsRepository.findTopCancellationSellers(filter.getFrom(), filter.getTo(), filter, 1);
        if (!topCancellers.isEmpty()) {
            Object[] row = topCancellers.get(0);
            long userCount = AnalyticsConstants.longValue(row[3]);
            if (userCount >= 5) {
                items.add(AnalyticsBusinessAlertRow.builder()
                        .code("USER_HIGH_CANCELLATIONS")
                        .severity("warning")
                        .title("Utilisateur avec beaucoup d'annulations")
                        .message((row[1] + " " + row[2]).trim() + " — " + userCount + " annulations")
                        .value(BigDecimal.valueOf(userCount))
                        .entityType("USER")
                        .entityId(((Number) row[0]).longValue())
                        .build());
            }
        }

        BigDecimal refunds = analyticsRepository.aggregateRefunds(filter.getFrom(), filter.getTo(), filter);
        var sales = analyticsRepository.aggregateSales(filter.getFrom(), filter.getTo(), filter);
        if (sales.revenue().compareTo(BigDecimal.ZERO) > 0
                && refunds.multiply(BigDecimal.valueOf(100)).divide(sales.revenue(), 2, java.math.RoundingMode.HALF_UP)
                .compareTo(BigDecimal.valueOf(10)) > 0) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("HIGH_REFUNDS")
                    .severity("warning")
                    .title("Remboursements élevés")
                    .message("Les remboursements dépassent 10 % du CA")
                    .value(refunds)
                    .build());
        }

        var cash = analyticsRepository.aggregateCashSessions(filter.getFrom(), filter.getTo(), filter);
        if (cash.difference().abs().compareTo(BigDecimal.valueOf(20)) > 0) {
            items.add(AnalyticsBusinessAlertRow.builder()
                    .code("CASH_DIFFERENCE")
                    .severity("critical")
                    .title("Écart de caisse important")
                    .message("Écart cumulé sur les sessions fermées")
                    .value(cash.difference())
                    .build());
        }

        return AnalyticsBusinessAlertsResponse.builder().items(items).build();
    }

    private AnalyticsStockAlertRow alert(Long id, String name, String sku, BigDecimal stock, BigDecimal sold,
                                         String type, String severity) {
        return AnalyticsStockAlertRow.builder()
                .productId(id)
                .productName(name)
                .sku(sku)
                .stockRemaining(stock)
                .quantitySoldPeriod(sold)
                .alertType(type)
                .severity(severity)
                .build();
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "critical" -> 0;
            case "warning" -> 1;
            default -> 2;
        };
    }
}
