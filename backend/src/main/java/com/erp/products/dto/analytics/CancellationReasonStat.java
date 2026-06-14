package com.erp.products.dto.analytics;

import com.erp.products.domain.enums.SaleCancellationReason;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CancellationReasonStat {

    private SaleCancellationReason reason;
    private String reasonLabel;
    private long count;
    private BigDecimal amount;
}
