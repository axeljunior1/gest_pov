package com.erp.products.domain.enums;

public enum PaymentMethod {
    CASH,
    CARD,
    MOBILE_MONEY,
    BANK_TRANSFER,
    OTHER,
    /** Contrepartie interne lors d'un echange (compense la valeur reprise contre le nouvel article) — jamais de vrai argent. */
    EXCHANGE_OFFSET
}
