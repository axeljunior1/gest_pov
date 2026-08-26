package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RefundValidateRequest {

    private List<SaleRefundRequest.RefundPaymentRequest> payments;
    /** Part du retour compensee par un echange (jamais un vrai paiement — pas de ligne RefundPayment). */
    private BigDecimal exchangeOffsetAmount;
    private String managerEmail;
    private String managerPassword;
    private String managerBadgeCode;
    private String managerPin;
}
