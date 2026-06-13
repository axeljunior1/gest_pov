package com.erp.products.service;

import com.erp.products.domain.enums.PosSalesFlowMode;
import com.erp.products.domain.enums.PosSessionType;
import com.erp.products.dto.PosConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosConfigService {

    private final SettingsService settingsService;

    public PosConfigResponse getConfig() {
        return settingsService.getPosConfig();
    }

    public PosSalesFlowMode getMode() {
        return PosSalesFlowMode.valueOf(getConfig().getSalesFlowMode());
    }

    public boolean isCentralCashier() {
        return getMode() == PosSalesFlowMode.CENTRAL_CASHIER;
    }

    public boolean isSellerCollectsPayment() {
        return getMode() == PosSalesFlowMode.SELLER_COLLECTS_PAYMENT;
    }

    public PosSessionType defaultSessionTypeForOpen() {
        return isCentralCashier() ? PosSessionType.SALES : PosSessionType.CASHIER;
    }

    public PosSessionType requiredSessionTypeForSaleCreation() {
        return isCentralCashier() ? PosSessionType.SALES : PosSessionType.CASHIER;
    }

    public PosSessionType requiredSessionTypeForPayment() {
        return PosSessionType.CASHIER;
    }
}
