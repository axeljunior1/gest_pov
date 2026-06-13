package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private String invoiceNumber;
    private Instant saleDate;
    private String companyName;
    private String registerName;
    private String sellerName;
    private String cashierName;
    private Long customerId;
    private String customerNumber;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String customerCity;
    private List<TicketResponse.TicketLine> lines;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal loyaltyDiscountAmount;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private List<PaymentResponse> payments;
    private BigDecimal changeAmount;
    private String currency;
}
