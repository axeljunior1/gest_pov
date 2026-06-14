package com.erp.products.dto;

import com.erp.products.domain.enums.SaleEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SaleEventResponse {

    private Long id;
    private SaleEventType eventType;
    private String eventTypeLabel;
    private String description;
    private String details;
    private String actorName;
    private Instant occurredAt;
}
