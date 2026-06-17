package com.erp.products.service.stockvaluation;



import com.erp.products.domain.entity.StockValuationMovement;

import com.erp.products.repository.StockValuationMovementRepository;

import com.erp.products.repository.StockValuationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import com.erp.products.dto.stockvaluation.*;



import java.math.BigDecimal;

import java.sql.Timestamp;

import java.time.Instant;

import java.time.LocalDate;

import java.time.YearMonth;

import java.time.ZoneId;

import java.time.temporal.ChronoUnit;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;



@Service

@RequiredArgsConstructor

public class StockValuationAnalyticsService {



    private final StockValuationRepository valuationRepository;

    private final StockValuationMovementRepository movementRepository;

    private final StockCmpValuationService cmpValuationService;



    @Transactional(readOnly = true)

    public StockValuationOverviewResponse getOverview() {

        BigDecimal totalValue = cmpValuationService.getTotalStockValue();

        List<Object[]> byCategory = movementRepository.sumValueByCategory();

        List<StockValuationCategoryRow> categories = byCategory.stream()

                .map(row -> StockValuationCategoryRow.builder()

                        .categoryId(row[0] != null ? ((Number) row[0]).longValue() : null)

                        .categoryName(row[1] != null ? row[1].toString() : "Sans catégorie")

                        .stockValue(toBigDecimal(row[2]))

                        .build())

                .toList();



        return StockValuationOverviewResponse.builder()

                .totalStockValue(totalValue)

                .byCategory(categories)

                .build();

    }



    @Transactional(readOnly = true)

    public List<StockValuationProductRow> getStockValueByProduct() {

        return valuationRepository.findAllWithStock().stream()

                .map(v -> StockValuationProductRow.builder()

                        .productId(v.getProduct().getId())

                        .productName(v.getProduct().getNom())

                        .variantId(v.getVariant() != null ? v.getVariant().getId() : null)

                        .variantLabel(v.getVariant() != null ? v.getVariant().getName() : null)

                        .quantityOnHand(v.getQuantityOnHand())

                        .averageUnitCost(v.getAverageUnitCost())

                        .stockValue(v.getStockValue())

                        .build())

                .toList();

    }



    @Transactional(readOnly = true)

    public List<StockValuationTrendPoint> getStockValueHistory(LocalDate from, LocalDate to, String granularity) {

        List<StockValuationTrendPoint> daily = buildDailyHistory(from, to);

        if ("month".equalsIgnoreCase(granularity)) {

            return aggregateByMonth(daily);

        }

        return daily;

    }



    @Transactional(readOnly = true)

    public BigDecimal getStockValueAtDate(LocalDate date) {

        ZoneId zone = ZoneId.systemDefault();

        Instant endExclusive = date.plusDays(1).atStartOfDay(zone).toInstant();

        Map<String, BigDecimal> values = new HashMap<>();

        for (StockValuationMovement movement : movementRepository.findByMovementDateBeforeOrderByMovementDateAscIdAsc(endExclusive)) {

            applyMovement(values, movement);

        }

        return sumValues(values);

    }



    @Transactional(readOnly = true)

    public List<StockValuationProductRow> getTopProductsByValue(int limit) {

        return movementRepository.topProductsByValue(limit).stream()

                .map(row -> StockValuationProductRow.builder()

                        .productId(((Number) row[0]).longValue())

                        .variantId(row[1] != null ? ((Number) row[1]).longValue() : null)

                        .productName(row[2].toString())

                        .stockValue(toBigDecimal(row[3]))

                        .build())

                .toList();

    }



    @Transactional(readOnly = true)

    public List<StockValuationStaleProductRow> getStaleProducts(int inactiveDays, int limit) {

        Instant before = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);

        return movementRepository.staleProducts(before, limit).stream()

                .map(row -> StockValuationStaleProductRow.builder()

                        .productId(((Number) row[0]).longValue())

                        .variantId(row[1] != null ? ((Number) row[1]).longValue() : null)

                        .productName(row[2].toString())

                        .quantityOnHand(toBigDecimal(row[3]))

                        .stockValue(toBigDecimal(row[4]))

                        .lastMovementAt(toInstant(row[5]))

                        .build())

                .toList();

    }



    private List<StockValuationTrendPoint> buildDailyHistory(LocalDate from, LocalDate to) {

        ZoneId zone = ZoneId.systemDefault();

        Instant endExclusive = to.plusDays(1).atStartOfDay(zone).toInstant();

        List<StockValuationMovement> movements =

                movementRepository.findByMovementDateBeforeOrderByMovementDateAscIdAsc(endExclusive);



        Map<String, BigDecimal> values = new HashMap<>();

        int index = 0;

        Instant fromInstant = from.atStartOfDay(zone).toInstant();

        while (index < movements.size() && movements.get(index).getMovementDate().isBefore(fromInstant)) {

            applyMovement(values, movements.get(index++));

        }



        List<StockValuationTrendPoint> points = new ArrayList<>();

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {

            Instant dayEndExclusive = day.plusDays(1).atStartOfDay(zone).toInstant();

            while (index < movements.size() && movements.get(index).getMovementDate().isBefore(dayEndExclusive)) {

                applyMovement(values, movements.get(index++));

            }

            points.add(StockValuationTrendPoint.builder()

                    .period(day.toString())

                    .value(sumValues(values))

                    .build());

        }

        return points;

    }



    private List<StockValuationTrendPoint> aggregateByMonth(List<StockValuationTrendPoint> daily) {

        Map<YearMonth, BigDecimal> lastValueByMonth = new LinkedHashMap<>();

        for (StockValuationTrendPoint point : daily) {

            YearMonth month = YearMonth.parse(point.getPeriod().substring(0, 7));

            lastValueByMonth.put(month, point.getValue());

        }

        List<StockValuationTrendPoint> monthly = new ArrayList<>();

        for (Map.Entry<YearMonth, BigDecimal> entry : lastValueByMonth.entrySet()) {

            monthly.add(StockValuationTrendPoint.builder()

                    .period(entry.getKey().toString())

                    .value(entry.getValue())

                    .build());

        }

        return monthly;

    }



    private static void applyMovement(Map<String, BigDecimal> values, StockValuationMovement movement) {

        values.put(variantKey(movement.getProduct().getId(), movement.getVariant() != null ? movement.getVariant().getId() : null),

                movement.getStockValueAfter());

    }



    private static String variantKey(Long productId, Long variantId) {

        return productId + ":" + (variantId != null ? variantId : 0);

    }



    private static BigDecimal sumValues(Map<String, BigDecimal> values) {

        return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    }



    private static BigDecimal toBigDecimal(Object value) {

        if (value == null) {

            return BigDecimal.ZERO;

        }

        if (value instanceof BigDecimal bd) {

            return bd;

        }

        return new BigDecimal(value.toString());

    }



    private static Instant toInstant(Object value) {

        if (value instanceof Instant i) {

            return i;

        }

        if (value instanceof Timestamp ts) {

            return ts.toInstant();

        }

        return Instant.parse(value.toString());

    }

}


