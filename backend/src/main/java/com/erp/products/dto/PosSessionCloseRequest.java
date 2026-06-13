package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSessionCloseRequest {

    /** Cash réellement présent en caisse (obligatoire pour session CASHIER). */
    private BigDecimal closingCashAmount;
    /** Si true, annule toutes les ventes brouillon de la session avant fermeture. */
    private Boolean cancelPendingDrafts;
    /** Motif obligatoire si écart ≠ 0 (code CashDifferenceReason). */
    private String differenceReason;
    private String differenceComment;
    /** Validation manager si paramètre activé et écart ≠ 0. */
    private String managerEmail;
    private String managerPassword;
}
