package com.erp.products.mapper;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PosMapper {

    public PosSessionResponse toSessionResponse(PosSession session) {
        return PosSessionResponse.builder()
                .id(session.getId())
                .sessionNumber(session.getSessionNumber())
                .cashierId(session.getCashier().getId())
                .cashierName(session.getCashier().fullName())
                .warehouseId(session.getWarehouse().getId())
                .warehouseCode(session.getWarehouse().getCode())
                .warehouseNom(session.getWarehouse().getNom())
                .openingCashAmount(session.getOpeningCashAmount())
                .closingCashAmount(session.getClosingCashAmount())
                .expectedCashAmount(session.getExpectedCashAmount())
                .differenceAmount(session.getDifferenceAmount())
                .status(session.getStatus())
                .sessionType(session.getSessionType())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .build();
    }

    public SaleResponse toSaleResponse(Sale sale) {
        User seller = sale.getSeller() != null ? sale.getSeller() : sale.getCashier();
        return SaleResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .posSessionId(sale.getPosSession().getId())
                .paymentSessionId(sale.getPaymentSession() != null ? sale.getPaymentSession().getId() : null)
                .sellerId(seller.getId())
                .sellerName(seller.fullName())
                .cashierId(sale.getCashier().getId())
                .cashierName(sale.getCashier().fullName())
                .warehouseId(sale.getWarehouse().getId())
                .status(sale.getStatus())
                .customerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null)
                .customerNumber(sale.getCustomer() != null ? sale.getCustomer().getCustomerNumber() : null)
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .customerPhone(sale.getCustomer() != null ? sale.getCustomer().getPhone() : null)
                .customerLoyaltyPoints(sale.getCustomer() != null ? sale.getCustomer().getLoyaltyPoints() : null)
                .customerLoyaltyTier(sale.getCustomer() != null ? sale.getCustomer().getLoyaltyTier() : null)
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .loyaltyDiscountAmount(sale.getLoyaltyDiscountAmount())
                .loyaltyPointsRedeemed(sale.getLoyaltyPointsRedeemed())
                .loyaltyPointsEarned(sale.getLoyaltyPointsEarned())
                .taxTotal(sale.getTaxTotal())
                .total(sale.getTotal())
                .paidAmount(sale.getPaidAmount())
                .changeAmount(sale.getChangeAmount())
                .holdLabel(sale.getHoldLabel())
                .createdAt(sale.getCreatedAt())
                .submittedAt(sale.getSubmittedAt())
                .sentToPaymentAt(sale.getSubmittedAt())
                .validatedAt(sale.getValidatedAt())
                .paidAt(sale.getPaidAt() != null ? sale.getPaidAt() : sale.getValidatedAt())
                .cancelledAt(sale.getCancelledAt())
                .lignes(sale.getLignes().stream().map(this::toLineResponse).collect(Collectors.toList()))
                .payments(sale.getPayments().stream().map(this::toPaymentResponse).collect(Collectors.toList()))
                .build();
    }

    public SaleLineResponse toLineResponse(SaleLine line) {
        return SaleLineResponse.builder()
                .id(line.getId())
                .productId(line.getProduct().getId())
                .productNom(line.getProduct().getNom())
                .productSku(line.getProduct().getSku())
                .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                .packagingId(line.getPackaging() != null ? line.getPackaging().getId() : null)
                .packagingNameSnapshot(line.getPackagingNameSnapshot())
                .packagingQuantitySnapshot(line.getPackagingQuantitySnapshot())
                .quantityInput(line.getQuantityInput())
                .quantityInBaseUnit(line.getQuantityInBaseUnit())
                .unitPrice(line.getUnitPrice())
                .unitPriceSnapshot(line.getUnitPriceSnapshot())
                .discountAmount(line.getDiscountAmount())
                .taxRate(line.getTaxRate())
                .lineTotal(line.getLineTotal())
                .build();
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .cashierId(payment.getCashier() != null ? payment.getCashier().getId() : null)
                .posSessionId(payment.getPosSession() != null ? payment.getPosSession().getId() : null)
                .build();
    }

    public SaleRefundResponse toRefundResponse(SaleRefund refund) {
        return SaleRefundResponse.builder()
                .id(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .saleId(refund.getSale().getId())
                .saleNumber(refund.getSale().getSaleNumber())
                .status(refund.getStatus())
                .totalAmount(refund.getTotalAmount())
                .reason(refund.getReason())
                .returnToStock(refund.getReturnToStock())
                .createdBy(refund.getCreatedBy())
                .createdAt(refund.getCreatedAt())
                .completedAt(refund.getCompletedAt())
                .lignes(refund.getLignes().stream().map(l -> SaleRefundLineResponse.builder()
                        .id(l.getId())
                        .saleLineId(l.getSaleLine().getId())
                        .quantity(l.getQuantity())
                        .refundAmount(l.getRefundAmount())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
