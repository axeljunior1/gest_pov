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
                .productNom(line.getProductNameSnapshot() != null
                        ? line.getProductNameSnapshot() : line.getProduct().getNom())
                .productSku(line.getProduct().getSku())
                .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                .variantNameSnapshot(line.getVariantNameSnapshot())
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
        Sale sale = refund.getSale();
        return SaleRefundResponse.builder()
                .id(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .saleId(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .status(refund.getStatus())
                .refundStatus(refund.getRefundStatus())
                .totalAmount(refund.getTotalAmount())
                .reason(refund.getReason())
                .notes(refund.getNotes())
                .returnToStock(refund.getReturnToStock())
                .createdBy(refund.getCreatedBy())
                .createdAt(refund.getCreatedAt())
                .validatedAt(refund.getValidatedAt())
                .completedAt(refund.getCompletedAt())
                .cancelledAt(refund.getCancelledAt())
                .lignes(refund.getLignes().stream().map(l -> {
                    Product p = l.getProduct() != null ? l.getProduct() : l.getSaleLine().getProduct();
                    return SaleRefundLineResponse.builder()
                            .id(l.getId())
                            .saleLineId(l.getSaleLine().getId())
                            .productId(p.getId())
                            .productNom(p.getNom())
                            .packagingId(l.getPackaging() != null ? l.getPackaging().getId() : null)
                            .packagingNameSnapshot(l.getPackagingNameSnapshot())
                            .quantity(l.getQuantity())
                            .quantityInBaseUnit(l.getQuantityInBaseUnit())
                            .unitPriceSnapshot(l.getUnitPriceSnapshot())
                            .refundAmount(l.getRefundAmount())
                            .restock(l.getRestock())
                            .reason(l.getReason())
                            .notes(l.getNotes())
                            .build();
                }).collect(Collectors.toList()))
                .payments(refund.getPayments().stream().map(p -> RefundPaymentResponse.builder()
                        .id(p.getId())
                        .method(p.getMethod())
                        .amount(p.getAmount())
                        .status(p.getStatus())
                        .refundedAt(p.getRefundedAt())
                        .originalPaymentId(p.getOriginalPayment() != null ? p.getOriginalPayment().getId() : null)
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
