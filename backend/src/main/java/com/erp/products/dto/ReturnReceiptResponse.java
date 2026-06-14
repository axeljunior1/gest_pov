package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReturnReceiptResponse {

    private String returnNumber;
    private String originalSaleNumber;
    private Instant returnDate;
    private String companyName;
    private String registerName;
    private String cashierName;
    private String customerName;
    private BigDecimal refundTotal;
    private String currency;
    private String reason;
    private List<ReceiptLine> lines;
    private List<ReceiptPayment> payments;

    @Data
    @Builder
    public static class ReceiptLine {
        private String productNom;
        private String packagingName;
        private BigDecimal quantity;
        private BigDecimal refundAmount;
        private boolean restock;
        private String reason;
    }

    @Data
    @Builder
    public static class ReceiptPayment {
        private PaymentMethod method;
        private BigDecimal amount;
    }
}
