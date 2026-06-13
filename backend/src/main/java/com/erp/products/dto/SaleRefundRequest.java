package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleRefundRequest {

    private String reason;
    private Boolean returnToStock;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long saleLineId;
        private BigDecimal quantity;
    }
}
