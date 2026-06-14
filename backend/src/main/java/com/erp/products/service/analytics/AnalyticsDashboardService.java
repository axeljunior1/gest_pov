package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.*;
import com.erp.products.repository.AnalyticsRepository;
import com.erp.products.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsDashboardService {

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;
    private final SettingsService settingsService;

    public AnalyticsOverviewResponse getOverview(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        var today = filterService.dayRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")));
        var week = filterService.weekRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")));
        var month = filterService.monthRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")));

        var todayMetrics = analyticsRepository.aggregateSales(today.from(), today.to(), filter);
        var yesterdayMetrics = analyticsRepository.aggregateSales(
                filterService.dayRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")).minusDays(1)).from(),
                today.from(),
                filter);
        var weekMetrics = analyticsRepository.aggregateSales(week.from(), week.to(), filter);
        var prevWeekMetrics = analyticsRepository.aggregateSales(
                filterService.weekRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")).minusWeeks(1)).from(),
                week.from(),
                filter);
        var monthMetrics = analyticsRepository.aggregateSales(month.from(), month.to(), filter);
        var prevMonthMetrics = analyticsRepository.aggregateSales(
                filterService.monthRange(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Paris")).minusMonths(1)).from(),
                month.from(),
                filter);

        var periodMetrics = analyticsRepository.aggregateSales(filter.getFrom(), filter.getTo(), filter);
        var prevPeriodMetrics = analyticsRepository.aggregateSales(filter.getCompareFrom(), filter.getCompareTo(), filter);

        BigDecimal refunds = analyticsRepository.aggregateRefunds(filter.getFrom(), filter.getTo(), filter);
        BigDecimal prevRefunds = analyticsRepository.aggregateRefunds(filter.getCompareFrom(), filter.getCompareTo(), filter);

        long cancelledCount = analyticsRepository.countCancelledSales(filter.getFrom(), filter.getTo(), filter);
        BigDecimal cancelledAmount = analyticsRepository.sumCancelledAmount(filter.getFrom(), filter.getTo(), filter);
        long prevCancelledCount = analyticsRepository.countCancelledSales(filter.getCompareFrom(), filter.getCompareTo(), filter);
        BigDecimal prevCancelledAmount = analyticsRepository.sumCancelledAmount(filter.getCompareFrom(), filter.getCompareTo(), filter);

        String currency = settingsService.getPublicSettings().getCurrency();

        return AnalyticsOverviewResponse.builder()
                .revenueToday(filterService.compare(todayMetrics.revenue(), yesterdayMetrics.revenue()))
                .revenueWeek(filterService.compare(weekMetrics.revenue(), prevWeekMetrics.revenue()))
                .revenueMonth(filterService.compare(monthMetrics.revenue(), prevMonthMetrics.revenue()))
                .salesCountToday(filterService.compare(todayMetrics.saleCount(), yesterdayMetrics.saleCount()))
                .averageBasketToday(filterService.compare(todayMetrics.averageBasket(), yesterdayMetrics.averageBasket()))
                .itemsSoldToday(filterService.compare(todayMetrics.itemsSold(), yesterdayMetrics.itemsSold()))
                .refundsTotal(refunds)
                .discountsTotal(periodMetrics.discounts().add(periodMetrics.loyaltyDiscounts()))
                .grossProfitEstimate(periodMetrics.grossProfit())
                .refundsPeriod(filterService.compare(refunds, prevRefunds))
                .discountsPeriod(filterService.compare(
                        periodMetrics.discounts().add(periodMetrics.loyaltyDiscounts()),
                        prevPeriodMetrics.discounts().add(prevPeriodMetrics.loyaltyDiscounts())))
                .cancelledCountPeriod(filterService.compare(cancelledCount, prevCancelledCount))
                .cancelledAmountPeriod(filterService.compare(cancelledAmount, prevCancelledAmount))
                .cancelledAmountTotal(cancelledAmount)
                .periodLabel(request.getPeriod() != null ? request.getPeriod() : "THIS_MONTH")
                .currency(currency)
                .build();
    }
}
