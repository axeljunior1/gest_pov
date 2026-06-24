package com.erp.products.mapper;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.service.SettingsService;
import com.erp.products.service.StockService;
import com.erp.products.service.ProductVariantAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PosMapper {

    private final StockService stockService;
    private final SettingsService settingsService;
    private final ProductVariantAttributeService variantAttributeService;

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
                .lignes(enrichLines(sale))
                .hasStockIssues(hasStockIssues(sale))
                .payments(sale.getPayments().stream().map(this::toPaymentResponse).collect(Collectors.toList()))
                .build();
    }

    public SaleLineResponse toLineResponse(SaleLine line) {
        return toLineResponse(line, null, false);
    }

    private List<SaleLineResponse> enrichLines(Sale sale) {
        Long warehouseId = sale.getWarehouse().getId();
        boolean checkStock = !settingsService.getStockConfig().isAllowNegativeStock();
        return sale.getLignes().stream()
                .map(line -> toLineResponse(line, warehouseId, checkStock))
                .collect(Collectors.toList());
    }

    private Boolean hasStockIssues(Sale sale) {
        if (settingsService.getStockConfig().isAllowNegativeStock()) {
            return false;
        }
        Long warehouseId = sale.getWarehouse().getId();
        return sale.getLignes().stream()
                .anyMatch(line -> isStockInsufficient(line, warehouseId));
    }

    private SaleLineResponse toLineResponse(SaleLine line, Long warehouseId, boolean checkStock) {
        BigDecimal stockAvailable = null;
        Boolean stockInsufficient = null;
        if (checkStock && warehouseId != null && isLineStockable(line)) {
            stockAvailable = stockService.getAvailable(
                    line.getProduct().getId(),
                    line.getVariant() != null ? line.getVariant().getId() : null,
                    warehouseId).getQuantityAvailable();
            stockInsufficient = line.getQuantityInBaseUnit().compareTo(stockAvailable) > 0;
        }

        return SaleLineResponse.builder()
                .id(line.getId())
                .productId(line.getProduct().getId())
                .productNom(line.getProductNameSnapshot() != null
                        ? line.getProductNameSnapshot() : line.getProduct().getNom())
                .productSku(line.getProduct().getSku())
                .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                .variantNameSnapshot(resolveVariantNameSnapshot(line))
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
                .stockAvailable(stockAvailable)
                .stockInsufficient(stockInsufficient)
                .unitCostAtSale(line.getUnitCostAtSale())
                .costOfGoodsSold(computeCostOfGoodsSold(line))
                .grossMargin(computeGrossMargin(line))
                .build();
    }

    private BigDecimal computeCostOfGoodsSold(SaleLine line) {
        if (line.getUnitCostAtSale() == null || line.getQuantityInBaseUnit() == null) {
            return null;
        }
        return line.getUnitCostAtSale().multiply(line.getQuantityInBaseUnit());
    }

    private BigDecimal computeGrossMargin(SaleLine line) {
        BigDecimal cogs = computeCostOfGoodsSold(line);
        if (cogs == null || line.getLineTotal() == null) {
            return null;
        }
        return line.getLineTotal().subtract(cogs);
    }

    private boolean isLineStockable(SaleLine line) {
        if (line.getVariant() != null) {
            return !Boolean.FALSE.equals(line.getVariant().getIsStockable());
        }
        return !Boolean.FALSE.equals(line.getProduct().getIsStockable());
    }

    private boolean isStockInsufficient(SaleLine line, Long warehouseId) {
        if (!isLineStockable(line)) {
            return false;
        }
        BigDecimal available = stockService.getAvailable(
                line.getProduct().getId(),
                line.getVariant() != null ? line.getVariant().getId() : null,
                warehouseId).getQuantityAvailable();
        return line.getQuantityInBaseUnit().compareTo(available) > 0;
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
                .customerName(resolveRefundCustomerName(refund, sale))
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
                            .variantNameSnapshot(l.getSaleLine().getVariantNameSnapshot())
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

    private String resolveRefundCustomerName(SaleRefund refund, Sale sale) {
        if (refund.getCustomer() != null) {
            return refund.getCustomer().fullName();
        }
        if (sale.getCustomer() != null) {
            return sale.getCustomer().fullName();
        }
        return null;
    }

    private String resolveVariantNameSnapshot(SaleLine line) {
        if (line.getVariantNameSnapshot() != null && !line.getVariantNameSnapshot().isBlank()) {
            return line.getVariantNameSnapshot().trim();
        }
        if (line.getVariant() != null) {
            return variantAttributeService.buildVariantLabel(line.getVariant());
        }
        return null;
    }
}
