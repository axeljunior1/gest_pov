package com.erp.products.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CancellationActorStat {

    private Long userId;
    private String userName;
    private long count;
    private BigDecimal amount;
}
