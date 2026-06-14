package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReturnableSaleResponse {

    private Long id;
    private String saleNumber;
    private Instant paidAt;
    private String customerName;
    private String customerNumber;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal amountAlreadyRefunded;
    private BigDecimal amountRefundable;
    private List<ReturnableLineResponse> lines;
}
