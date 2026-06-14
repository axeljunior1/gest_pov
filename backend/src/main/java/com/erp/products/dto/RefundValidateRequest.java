package com.erp.products.dto;

import lombok.Data;

import java.util.List;

@Data
public class RefundValidateRequest {

    private List<SaleRefundRequest.RefundPaymentRequest> payments;
    private String managerEmail;
    private String managerPassword;
}
