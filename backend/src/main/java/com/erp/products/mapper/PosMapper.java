package com.erp.products.mapper;

import com.erp.products.domain.entity.*;
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
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .build();
    }

    public SaleResponse toSaleResponse(Sale sale) {
        return SaleResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .posSessionId(sale.getPosSession().getId())
                .cashierId(sale.getCashier().getId())
                .cashierName(sale.getCashier().fullName())
                .warehouseId(sale.getWarehouse().getId())
                .status(sale.getStatus())
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .taxTotal(sale.getTaxTotal())
                .total(sale.getTotal())
                .paidAmount(sale.getPaidAmount())
                .changeAmount(sale.getChangeAmount())
                .holdLabel(sale.getHoldLabel())
                .createdAt(sale.getCreatedAt())
                .validatedAt(sale.getValidatedAt())
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
                .quantityInput(line.getQuantityInput())
                .quantityInBaseUnit(line.getQuantityInBaseUnit())
                .unitPrice(line.getUnitPrice())
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
