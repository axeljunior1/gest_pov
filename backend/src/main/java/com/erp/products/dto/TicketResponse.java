package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TicketResponse {

    private String ticketNumber;
    private Instant saleDate;
    private String companyName;
    private String registerName;
    private String cashierName;
    private List<TicketLine> lines;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private List<PaymentResponse> payments;
    private BigDecimal changeAmount;
    private String currency;
    private String companyLogoUrl;
    private String companyAddress;
    private String companyPhone;
    private String ticketFooter;
    private String taxName;
    private boolean pricesIncludeTax;

    @Data
    @Builder
    public static class TicketLine {
        private String productNom;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal lineTotal;
    }
}
