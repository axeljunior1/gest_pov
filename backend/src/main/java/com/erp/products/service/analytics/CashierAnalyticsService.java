package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashierAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;

    public AnalyticsCashiersResponse getCashiers(AnalyticsFilterRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!AnalyticsAuthHelper.has(auth, "analytics.cashier.read") && !AnalyticsAuthHelper.has(auth, "analytics.read")) {
            throw new BusinessException("Permission analytics.cashier.read requise");
        }

        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        List<Object[]> rows = analyticsRepository.findCashierStats(filter.getFrom(), filter.getTo(), filter);
        List<AnalyticsCashierRow> items = new ArrayList<>();

        for (Object[] row : rows) {
            Long cashierId = ((Number) row[0]).longValue();
            var refunds = analyticsRepository.aggregateRefunds(filter.getFrom(), filter.getTo(),
                    ResolvedAnalyticsFilter.builder()
                            .from(filter.getFrom())
                            .to(filter.getTo())
                            .cashierId(cashierId)
                            .warehouseId(filter.getWarehouseId())
                            .build());
            long cancellations = analyticsRepository.countCancelledSales(filter.getFrom(), filter.getTo(),
                    ResolvedAnalyticsFilter.builder()
                            .from(filter.getFrom())
                            .to(filter.getTo())
                            .cashierId(cashierId)
                            .warehouseId(filter.getWarehouseId())
                            .build());

            items.add(AnalyticsCashierRow.builder()
                    .cashierId(cashierId)
                    .cashierName((String) row[1])
                    .saleCount(AnalyticsConstants.longValue(row[2]))
                    .revenue(AnalyticsConstants.decimalValue(row[3]))
                    .averageBasket(AnalyticsConstants.decimalValue(row[4]))
                    .discountsGranted(AnalyticsConstants.decimalValue(row[5]))
                    .refundsTotal(refunds)
                    .cancellations(cancellations)
                    .cashDifference(BigDecimal.ZERO)
                    .build());
        }

        return AnalyticsCashiersResponse.builder().items(items).build();
    }

    public AnalyticsCustomerSummaryResponse getCustomers(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        long newCustomers = analyticsRepository.countNewCustomers(filter.getFrom(), filter.getTo());
        long returning = analyticsRepository.countReturningCustomers(filter.getFrom(), filter.getTo(), filter);
        long earned = analyticsRepository.sumLoyaltyPoints(filter.getFrom(), filter.getTo(), "EARN");
        long redeemed = analyticsRepository.sumLoyaltyPoints(filter.getFrom(), filter.getTo(), "REDEEM");

        List<Object[]> topRows = analyticsRepository.findTopCustomers(filter.getFrom(), filter.getTo(), filter, 10);
        List<AnalyticsTopCustomerRow> top = new ArrayList<>();
        for (Object[] row : topRows) {
            top.add(AnalyticsTopCustomerRow.builder()
                    .customerId(((Number) row[0]).longValue())
                    .customerName((String) row[1])
                    .customerNumber((String) row[2])
                    .revenue(AnalyticsConstants.decimalValue(row[3]))
                    .purchaseCount(AnalyticsConstants.longValue(row[4]))
                    .averageBasket(AnalyticsConstants.decimalValue(row[5]))
                    .build());
        }

        BigDecimal avgBasket = top.isEmpty() ? BigDecimal.ZERO : top.stream()
                .map(AnalyticsTopCustomerRow::getAverageBasket)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(top.size()), 2, java.math.RoundingMode.HALF_UP);

        return AnalyticsCustomerSummaryResponse.builder()
                .newCustomers(newCustomers)
                .returningCustomers(returning)
                .topCustomers(top)
                .averageBasketPerCustomer(avgBasket)
                .loyaltyPointsEarned(earned)
                .loyaltyPointsRedeemed(redeemed)
                .build();
    }
}
