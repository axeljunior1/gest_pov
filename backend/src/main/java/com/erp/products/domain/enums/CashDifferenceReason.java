package com.erp.products.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashDifferenceReason {
    CHANGE_ERROR("Erreur de rendu monnaie"),
    INPUT_ERROR("Erreur de saisie"),
    FORGOTTEN_PAYMENT("Paiement oublié"),
    LOST_TICKET("Perte de ticket"),
    COUNT_ERROR("Erreur de comptage"),
    OTHER("Autre");

    private final String label;

    public static CashDifferenceReason parse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return CashDifferenceReason.valueOf(code.trim().toUpperCase());
    }
}
