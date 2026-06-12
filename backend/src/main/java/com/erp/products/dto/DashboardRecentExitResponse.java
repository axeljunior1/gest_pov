package com.erp.products.dto;

import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class DashboardRecentExitResponse {

    private Long id;
    private String exitNumber;
    private String warehouseCode;
    private StockExitStatus status;
    private StockExitReason reason;
    private LocalDate exitDate;
    private Instant validatedAt;
    private int lineCount;
}
