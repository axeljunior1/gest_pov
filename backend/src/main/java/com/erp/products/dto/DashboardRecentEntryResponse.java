package com.erp.products.dto;

import com.erp.products.domain.enums.StockEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class DashboardRecentEntryResponse {

    private Long id;
    private String entryNumber;
    private String warehouseCode;
    private StockEntryStatus status;
    private LocalDate entryDate;
    private Instant validatedAt;
    private int lineCount;
}
