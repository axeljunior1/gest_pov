package com.erp.products.dto;

import com.erp.products.domain.enums.PaymentMethod;
import lombok.Data;

import java.util.List;

@Data
public class SaleExchangeRequest {

    /** Lignes reprises sur la vente d'origine (meme forme qu'un retour classique). */
    private List<SaleRefundRequest.Line> returnLines;

    /** Nouveaux articles donnes en remplacement. */
    private List<SaleLineRequest> newLines;

    private String reason;

    /** Methode reelle pour la difference (montant du a payer par le client, ou a lui rembourser). */
    private PaymentMethod paymentMethod;

    private String managerEmail;
    private String managerPassword;
    private String managerBadgeCode;
    private String managerPin;
}
