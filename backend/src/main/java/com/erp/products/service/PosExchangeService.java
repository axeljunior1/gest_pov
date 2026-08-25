package com.erp.products.service;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.entity.SaleExchange;
import com.erp.products.domain.entity.SaleRefund;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.SaleExchangeRepository;
import com.erp.products.repository.SaleRefundRepository;
import com.erp.products.repository.SaleRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Echange (reprise d'un article + vente d'un autre) traite comme UNE seule operation tracee,
 * au lieu d'un retour et d'une vente sans lien entre eux. Reutilise PosRefundService et
 * PosSaleService tels quels (memes regles, memes controles anti-fraude) — cette classe orchestre
 * seulement le calcul de la difference nette et le lien entre les deux enregistrements.
 */
@Service
@RequiredArgsConstructor
public class PosExchangeService {

    private final PosRefundService refundService;
    private final PosSaleService saleService;
    private final SaleRepository saleRepository;
    private final SaleRefundRepository refundRepository;
    private final SaleExchangeRepository exchangeRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional
    public SaleExchangeResponse create(Long originalSaleId, SaleExchangeRequest request) {
        if (request.getReturnLines() == null || request.getReturnLines().isEmpty()) {
            throw new BusinessException("Sélectionnez au moins une ligne à reprendre");
        }
        if (request.getNewLines() == null || request.getNewLines().isEmpty()) {
            throw new BusinessException("Ajoutez au moins un article de remplacement");
        }
        PaymentMethod realMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH;

        SaleRefundRequest refundRequest = new SaleRefundRequest();
        refundRequest.setReason(request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason() : "Echange");
        refundRequest.setLines(request.getReturnLines());
        SaleRefundResponse refundDraft = refundService.createReturn(originalSaleId, refundRequest);
        BigDecimal returnTotal = refundDraft.getTotalAmount();

        SaleResponse newSale = saleService.createSale();
        for (SaleLineRequest line : request.getNewLines()) {
            newSale = saleService.upsertLine(newSale.getId(), line);
        }
        BigDecimal newItemsTotal = newSale.getTotal() != null ? newSale.getTotal() : BigDecimal.ZERO;

        BigDecimal netAmount = newItemsTotal.subtract(returnTotal);
        BigDecimal offset = returnTotal.min(newItemsTotal);

        RefundValidateRequest refundValidate = new RefundValidateRequest();
        List<SaleRefundRequest.RefundPaymentRequest> refundPayments = new ArrayList<>();
        if (offset.compareTo(BigDecimal.ZERO) > 0) {
            refundPayments.add(refundPayment(PaymentMethod.EXCHANGE_OFFSET, offset));
        }
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundPayments.add(refundPayment(realMethod, netAmount.abs()));
        }
        refundValidate.setPayments(refundPayments);
        refundValidate.setManagerEmail(request.getManagerEmail());
        refundValidate.setManagerPassword(request.getManagerPassword());
        refundValidate.setManagerBadgeCode(request.getManagerBadgeCode());
        refundValidate.setManagerPin(request.getManagerPin());
        SaleRefundResponse refund = refundService.validateReturn(refundDraft.getId(), refundValidate);

        SaleValidateRequest saleValidate = new SaleValidateRequest();
        List<SaleValidateRequest.PaymentInput> salePayments = new ArrayList<>();
        if (offset.compareTo(BigDecimal.ZERO) > 0) {
            salePayments.add(salePayment(PaymentMethod.EXCHANGE_OFFSET, offset));
        }
        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            salePayments.add(salePayment(realMethod, netAmount));
        }
        saleValidate.setPayments(salePayments);
        SaleResponse completedSale = saleService.validateSale(newSale.getId(), saleValidate);

        Sale original = saleRepository.findById(originalSaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvée: " + originalSaleId));
        Sale newSaleEntity = saleRepository.findById(completedSale.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvée: " + completedSale.getId()));
        SaleRefund refundEntity = refundRepository.findById(refund.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Retour non trouvé: " + refund.getId()));

        String actor = currentUserService.getCurrentUserEmailOrDefault();
        SaleExchange exchange = SaleExchange.builder()
                .exchangeNumber(generateExchangeNumber())
                .originalSale(original)
                .refund(refundEntity)
                .newSale(newSaleEntity)
                .netAmount(netAmount)
                .createdBy(actor)
                .build();
        SaleExchange saved = exchangeRepository.save(exchange);

        auditService.log("SaleExchange", saved.getId(), AuditAction.CREATION,
                "Echange créé " + saved.getExchangeNumber() + " (reprise " + refund.getRefundNumber()
                        + " / nouvelle vente " + completedSale.getSaleNumber() + ")", actor);

        return SaleExchangeResponse.builder()
                .id(saved.getId())
                .exchangeNumber(saved.getExchangeNumber())
                .originalSaleId(original.getId())
                .originalSaleNumber(original.getSaleNumber())
                .refundId(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .returnTotal(returnTotal)
                .newSaleId(completedSale.getId())
                .newSaleNumber(completedSale.getSaleNumber())
                .newItemsTotal(newItemsTotal)
                .netAmount(netAmount)
                .createdBy(saved.getCreatedBy())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private static SaleRefundRequest.RefundPaymentRequest refundPayment(PaymentMethod method, BigDecimal amount) {
        SaleRefundRequest.RefundPaymentRequest p = new SaleRefundRequest.RefundPaymentRequest();
        p.setMethod(method);
        p.setAmount(amount);
        return p;
    }

    private static SaleValidateRequest.PaymentInput salePayment(PaymentMethod method, BigDecimal amount) {
        SaleValidateRequest.PaymentInput p = new SaleValidateRequest.PaymentInput();
        p.setMethod(method);
        p.setAmount(amount);
        return p;
    }

    private String generateExchangeNumber() {
        String prefix = "ECH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = exchangeRepository.findTopByExchangeNumberStartingWithOrderByExchangeNumberDesc(prefix)
                .map(e -> {
                    String suffix = e.getExchangeNumber().substring(prefix.length());
                    try {
                        return Long.parseLong(suffix);
                    } catch (NumberFormatException ex) {
                        return 0L;
                    }
                })
                .orElse(0L) + 1;
        return prefix + String.format("%04d", seq);
    }
}
