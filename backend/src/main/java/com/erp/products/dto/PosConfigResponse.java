package com.erp.products.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PosConfigResponse {

    private String salesFlowMode;
    /** Compatibilite clients existants */
    private String cashHandlingMode;
    private boolean allowSellerCashCollection;
    private boolean allowPartialPayment;
    private boolean allowSplitPayment;
    private int maxPendingPaymentDurationMinutes;
    private int alertPendingPaymentMinutes;
    private int alertCashDifferenceThreshold;
    private boolean requireManagerValidationForCashDifference;
}
