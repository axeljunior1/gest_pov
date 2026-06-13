package com.erp.products.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSessionCloseRequest {

    private BigDecimal closingCashAmount;
    /** Si true, annule toutes les ventes brouillon de la session avant fermeture. */
    private Boolean cancelPendingDrafts;
}
