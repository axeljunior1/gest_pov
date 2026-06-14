package com.erp.products.domain.enums;

import lombok.Getter;

@Getter
public enum SaleCancellationReason {
    DATA_ENTRY_ERROR("Erreur de saisie"),
    QUANTITY_ERROR("Erreur de quantité"),
    PRODUCT_ERROR("Erreur de produit"),
    CUSTOMER_CHANGED_MIND("Client a changé d'avis"),
    DUPLICATE("Doublon"),
    PAYMENT_ABANDONED("Paiement abandonné"),
    CASHIER_ERROR("Erreur caissier"),
    SESSION_AUTO("Annulation automatique (fermeture session)"),
    OTHER("Autre");

    private final String label;

    SaleCancellationReason(String label) {
        this.label = label;
    }
}
