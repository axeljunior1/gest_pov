package com.erp.products.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public final class SaleStatuses {

    public static final Set<SaleStatus> PAID_OR_VALIDATED = EnumSet.of(
            SaleStatus.PAID,
            SaleStatus.VALIDATED
    );

    public static final Set<SaleStatus> COUNTED_FOR_REVENUE = EnumSet.of(
            SaleStatus.PAID,
            SaleStatus.VALIDATED,
            SaleStatus.PARTIALLY_REFUNDED,
            SaleStatus.REFUNDED
    );

    private SaleStatuses() {
    }

    public static boolean isPaid(SaleStatus status) {
        return status == SaleStatus.PAID || status == SaleStatus.VALIDATED;
    }

    public static boolean countsForRevenue(SaleStatus status) {
        return COUNTED_FOR_REVENUE.contains(status);
    }

    public static boolean stockWasDecremented(SaleStatus status) {
        return isPaid(status) || status == SaleStatus.PARTIALLY_REFUNDED || status == SaleStatus.REFUNDED;
    }
}
