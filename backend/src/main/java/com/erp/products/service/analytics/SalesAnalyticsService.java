package com.erp.products.service.analytics;

import com.erp.products.dto.analytics.AnalyticsFilterRequest;
import com.erp.products.dto.analytics.AnalyticsTimelinePoint;
import com.erp.products.dto.analytics.AnalyticsTimelineResponse;
import com.erp.products.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAnalyticsService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final AnalyticsRepository analyticsRepository;
    private final AnalyticsFilterService filterService;

    public AnalyticsTimelineResponse getTimeline(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);
        List<Object[]> rows = analyticsRepository.findTimelineSales(filter.getFrom(), filter.getTo(), filter);
        String granularity = filter.getGranularity() != null ? filter.getGranularity() : "DAY";

        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Instant at = (Instant) row[0];
            BigDecimal total = AnalyticsConstants.decimalValue(row[1]);
            String key = bucketKey(at, granularity);
            buckets.computeIfAbsent(key, k -> new Bucket(labelFor(key, granularity)))
                    .add(total);
        }

        List<AnalyticsTimelinePoint> points = new ArrayList<>();
        buckets.forEach((k, b) -> points.add(AnalyticsTimelinePoint.builder()
                .label(b.label)
                .revenue(b.revenue)
                .saleCount(b.count)
                .build()));

        return AnalyticsTimelineResponse.builder()
                .granularity(granularity)
                .points(points)
                .build();
    }

    private String bucketKey(Instant instant, String granularity) {
        var zdt = instant.atZone(ZONE);
        return switch (granularity) {
            case "HOUR" -> zdt.truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "MONTH" -> zdt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default -> zdt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        };
    }

    private String labelFor(String key, String granularity) {
        return switch (granularity) {
            case "HOUR" -> key.length() >= 16 ? key.substring(11, 16) + "h" : key;
            case "MONTH" -> key;
            default -> key;
        };
    }

    private static class Bucket {
        private final String label;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long count;

        Bucket(String label) {
            this.label = label;
        }

        void add(BigDecimal amount) {
            revenue = revenue.add(amount);
            count++;
        }
    }
}
