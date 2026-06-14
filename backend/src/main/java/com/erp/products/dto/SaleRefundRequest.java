package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleRefundRequest {

    private String reason;
    private String notes;
    /** @deprecated prefer per-line restock */
    private Boolean returnToStock;
    private List<Line> lines;
    private List<RefundPaymentRequest> payments;
    private String managerEmail;
    private String managerPassword;

    @Data
    public static class Line {
        private Long saleLineId;
        private BigDecimal quantity;
        private Boolean restock;
        private String reason;
        private String notes;
    }

    @Data
    public static class RefundPaymentRequest {
        private PaymentMethod method;
        private BigDecimal amount;
        private Long originalPaymentId;
    }
}
