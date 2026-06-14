package com.erp.products.domain.enums;

import lombok.Getter;

@Getter
public enum SaleEventType {
    CREATED("Création vente"),
    LINE_ADDED("Ajout produit"),
    LINE_UPDATED("Modification ligne"),
    LINE_REMOVED("Suppression ligne"),
    CUSTOMER_ASSIGNED("Client associé"),
    CUSTOMER_REMOVED("Client retiré"),
    LOYALTY_APPLIED("Fidélité appliquée"),
    LOYALTY_CLEARED("Fidélité retirée"),
    HOLD("Mise en pause"),
    RESUMED("Reprise vente"),
    SENT_TO_CASHIER("Envoi caisse"),
    RECALLED_FROM_CASHIER("Retour vendeur"),
    PAYMENT_STARTED("Paiement commencé"),
    PAYMENT_VALIDATED("Paiement validé"),
    CANCELLED("Annulation");

    private final String label;

    SaleEventType(String label) {
        this.label = label;
    }
}
