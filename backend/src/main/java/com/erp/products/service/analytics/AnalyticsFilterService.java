package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.AnalyticsComparisonMetric;
import com.erp.products.dto.analytics.AnalyticsFilterRequest;
import com.erp.products.exception.BusinessException;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class AnalyticsFilterService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final CurrentUserService currentUserService;

    public ResolvedAnalyticsFilter resolve(AnalyticsFilterRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRead = AnalyticsAuthHelper.has(auth, "analytics.read");
        boolean hasSales = AnalyticsAuthHelper.has(auth, "analytics.sales.read");
        boolean hasStock = AnalyticsAuthHelper.has(auth, "analytics.stock.read");
        boolean hasCashier = AnalyticsAuthHelper.has(auth, "analytics.cashier.read");

        if (!hasRead && !hasSales && !hasStock && !hasCashier) {
            throw new BusinessException("Permission analytics insuffisante");
        }

        boolean cashierOnly = hasSales && !hasRead && !hasCashier && !hasStock;

        String period = request.getPeriod() != null ? request.getPeriod().toUpperCase() : "THIS_MONTH";
        LocalDate today = LocalDate.now(ZONE);

        Range main = resolvePeriodRange(period, today, request.getDateFrom(), request.getDateTo());
        Range compare = compareRange(period, main, today);

        Long cashierId = request.getCashierId();
        boolean scoped = cashierOnly;
        Long scopedCashierId = null;
        if (scoped) {
            scopedCashierId = currentUserService.requireCurrentUser().getId();
            cashierId = scopedCashierId;
            main = dayRange(today);
            compare = dayRange(today.minusDays(1));
        }

        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), 100) : 20;

        return ResolvedAnalyticsFilter.builder()
                .from(main.from())
                .to(main.to())
                .compareFrom(compare.from())
                .compareTo(compare.to())
                .warehouseId(request.getWarehouseId())
                .cashierId(cashierId)
                .categoryId(request.getCategoryId())
                .productId(request.getProductId())
                .customerId(request.getCustomerId())
                .paymentMethod(request.getPaymentMethod())
                .granularity(request.getGranularity() != null ? request.getGranularity().toUpperCase() : "DAY")
                .page(page)
                .size(size)
                .sort(request.getSort())
                .cashierScoped(scoped)
                .scopedCashierId(scopedCashierId)
                .build();
    }

    public ResolvedAnalyticsFilter forPreset(String preset) {
        AnalyticsFilterRequest req = new AnalyticsFilterRequest();
        req.setPeriod(preset);
        return resolve(req);
    }

    public Range dayRange(LocalDate day) {
        return new Range(day.atStartOfDay(ZONE).toInstant(), day.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    public Range weekRange(LocalDate ref) {
        LocalDate start = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Range(start.atStartOfDay(ZONE).toInstant(), start.plusDays(7).atStartOfDay(ZONE).toInstant());
    }

    public Range monthRange(LocalDate ref) {
        LocalDate start = ref.withDayOfMonth(1);
        return new Range(start.atStartOfDay(ZONE).toInstant(), start.plusMonths(1).atStartOfDay(ZONE).toInstant());
    }

    public Range yearRange(LocalDate ref) {
        LocalDate start = ref.withDayOfYear(1);
        return new Range(start.atStartOfDay(ZONE).toInstant(), start.plusYears(1).atStartOfDay(ZONE).toInstant());
    }

    public AnalyticsComparisonMetric compare(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current != null ? current : BigDecimal.ZERO;
        BigDecimal prev = previous != null ? previous : BigDecimal.ZERO;
        BigDecimal change = BigDecimal.ZERO;
        String trend = "flat";
        if (prev.compareTo(BigDecimal.ZERO) > 0) {
            change = cur.subtract(prev)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prev, 2, RoundingMode.HALF_UP);
            trend = change.compareTo(BigDecimal.ZERO) > 0 ? "up" : change.compareTo(BigDecimal.ZERO) < 0 ? "down" : "flat";
        } else if (cur.compareTo(BigDecimal.ZERO) > 0) {
            change = BigDecimal.valueOf(100);
            trend = "up";
        }
        return AnalyticsComparisonMetric.builder()
                .current(cur)
                .previous(prev)
                .changePercent(change)
                .trend(trend)
                .build();
    }

    public AnalyticsComparisonMetric compare(long current, long previous) {
        return compare(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private Range resolvePeriodRange(String period, LocalDate today, Instant customFrom, Instant customTo) {
        return switch (period) {
            case "TODAY" -> dayRange(today);
            case "YESTERDAY" -> dayRange(today.minusDays(1));
            case "THIS_WEEK" -> weekRange(today);
            case "THIS_MONTH" -> monthRange(today);
            case "LAST_MONTH" -> monthRange(today.minusMonths(1));
            case "THIS_YEAR" -> yearRange(today);
            case "CUSTOM" -> {
                if (customFrom == null || customTo == null) {
                    throw new BusinessException("dateFrom et dateTo requis pour une période personnalisée");
                }
                yield new Range(customFrom, customTo);
            }
            default -> monthRange(today);
        };
    }

    private Range compareRange(String period, Range main, LocalDate today) {
        return switch (period) {
            case "TODAY" -> dayRange(today.minusDays(1));
            case "YESTERDAY" -> dayRange(today.minusDays(2));
            case "THIS_WEEK" -> weekRange(today.minusWeeks(1));
            case "THIS_MONTH" -> monthRange(today.minusMonths(1));
            case "LAST_MONTH" -> monthRange(today.minusMonths(2));
            case "THIS_YEAR" -> yearRange(today.minusYears(1));
            case "CUSTOM" -> {
                Duration d = Duration.between(main.from(), main.to());
                Instant compareTo = main.from();
                yield new Range(compareTo.minus(d), compareTo);
            }
            default -> monthRange(today.minusMonths(1));
        };
    }

    public record Range(Instant from, Instant to) {
    }
}
