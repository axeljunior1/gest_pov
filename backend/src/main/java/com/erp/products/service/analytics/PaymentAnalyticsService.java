package com.erp.products.service.analytics;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.dto.analytics.*;
import com.erp.products.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;

    public AnalyticsPaymentsResponse getPayments(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        List<Object[]> rows = analyticsRepository.findPaymentStats(filter.getFrom(), filter.getTo(), filter);
        BigDecimal total = rows.stream()
                .map(r -> AnalyticsConstants.decimalValue(r[1]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalyticsPaymentMethodRow> methods = new ArrayList<>();
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal amount = AnalyticsConstants.decimalValue(row[1]);
            long count = AnalyticsConstants.longValue(row[2]);
            BigDecimal share = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            methods.add(AnalyticsPaymentMethodRow.builder()
                    .method(method.name())
                    .methodLabel(label(method))
                    .total(amount)
                    .transactionCount(count)
                    .sharePercent(share)
                    .build());
        }

        var cash = analyticsRepository.aggregateCashSessions(filter.getFrom(), filter.getTo(), filter);
        BigDecimal refunds = analyticsRepository.aggregateRefunds(filter.getFrom(), filter.getTo(), filter);

        return AnalyticsPaymentsResponse.builder()
                .methods(methods)
                .refundsTotal(refunds)
                .expectedCash(cash.expectedCash())
                .declaredCash(cash.declaredCash())
                .cashDifference(cash.difference())
                .build();
    }

    private String label(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Espèces";
            case CARD -> "Carte";
            case MOBILE_MONEY -> "Mobile money";
            case BANK_TRANSFER -> "Virement";
            case OTHER -> "Autre";
        };
    }
}
