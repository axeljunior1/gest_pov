package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.*;
import com.erp.products.repository.AnalyticsRepository;
import com.erp.products.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;
    private final StockItemRepository stockItemRepository;

    public AnalyticsPagedProductsResponse getTopProducts(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        var pageable = PageRequest.of(filter.getPage(), filter.getSize());
        List<Object[]> rows = analyticsRepository.findTopProducts(filter.getFrom(), filter.getTo(), filter, pageable);
        long total = analyticsRepository.countTopProducts(filter.getFrom(), filter.getTo(), filter);

        List<AnalyticsProductRow> items = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            BigDecimal qty = AnalyticsConstants.decimalValue(row[4]);
            BigDecimal stock = stockItemRepository.sumQuantityOnHandByProductId(productId);
            BigDecimal rotation = stock.compareTo(BigDecimal.ZERO) > 0
                    ? qty.divide(stock, 2, RoundingMode.HALF_UP)
                    : qty;

            items.add(AnalyticsProductRow.builder()
                    .productId(productId)
                    .productName((String) row[1])
                    .sku((String) row[2])
                    .categoryName((String) row[3])
                    .quantitySold(qty)
                    .revenue(AnalyticsConstants.decimalValue(row[5]))
                    .estimatedMargin(AnalyticsConstants.decimalValue(row[6]))
                    .discountAmount(AnalyticsConstants.decimalValue(row[7]))
                    .stockRemaining(stock)
                    .rotationRate(rotation)
                    .build());
        }

        return AnalyticsPagedProductsResponse.builder()
                .items(items)
                .totalElements(total)
                .page(filter.getPage())
                .size(filter.getSize())
                .build();
    }

    public AnalyticsCategoriesResponse getCategories(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        List<Object[]> rows = analyticsRepository.findCategoryStats(filter.getFrom(), filter.getTo(), filter);
        BigDecimal totalRevenue = rows.stream()
                .map(r -> AnalyticsConstants.decimalValue(r[2]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalyticsCategoryRow> items = new ArrayList<>();
        for (Object[] row : rows) {
            BigDecimal revenue = AnalyticsConstants.decimalValue(row[2]);
            BigDecimal share = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            items.add(AnalyticsCategoryRow.builder()
                    .categoryId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .categoryName(row[1] != null ? (String) row[1] : "Sans catégorie")
                    .revenue(revenue)
                    .quantitySold(AnalyticsConstants.decimalValue(row[3]))
                    .estimatedMargin(AnalyticsConstants.decimalValue(row[4]))
                    .sharePercent(share)
                    .build());
        }
        return AnalyticsCategoriesResponse.builder().items(items).build();
    }
}
