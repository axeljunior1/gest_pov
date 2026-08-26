package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaleValidateRequest {

    private List<PaymentInput> payments;
    /** Montant reçu en espèces (pour calcul monnaie). */
    private BigDecimal cashReceived;
    /** Part du montant compensee par un echange (jamais un vrai paiement — pas de ligne Payment). */
    private BigDecimal exchangeOffsetAmount;

    @Data
    public static class PaymentInput {
        private PaymentMethod method;
        private BigDecimal amount;
    }
}
