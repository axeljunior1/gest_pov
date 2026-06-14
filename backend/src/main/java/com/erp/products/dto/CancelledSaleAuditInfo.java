package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CancelledSaleAuditInfo {

    private String createdByName;
    private Instant createdAt;
    private String updatedByName;
    private Instant lastUpdatedAt;
    private String cancelledByName;
    private Instant cancelledAt;
}
