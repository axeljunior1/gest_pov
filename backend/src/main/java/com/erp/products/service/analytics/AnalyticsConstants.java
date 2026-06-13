package com.erp.products.service.analytics;

import com.erp.products.domain.enums.SaleStatus;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

public final class AnalyticsConstants {

    public static final Set<SaleStatus> COUNTED_SALE_STATUSES = EnumSet.of(
            SaleStatus.PAID,
            SaleStatus.VALIDATED,
            SaleStatus.PARTIALLY_REFUNDED,
            SaleStatus.REFUNDED
    );

    private AnalyticsConstants() {
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public static long longValue(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }

    public static BigDecimal decimalValue(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(o.toString());
    }
}
