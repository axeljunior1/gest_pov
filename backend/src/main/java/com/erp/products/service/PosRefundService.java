package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.SaleRefundRequest;
import com.erp.products.dto.SaleRefundResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.SaleRefundRepository;
import com.erp.products.repository.SaleRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PosRefundService {

    private final SaleRefundRepository refundRepository;
    private final SaleRepository saleRepository;
    private final StockLedgerService ledger;
    private final CurrentUserService currentUserService;
    private final PosMapper mapper;
    private final AuditService auditService;

    @Transactional
    public SaleRefundResponse createRefund(Long saleId, SaleRefundRequest request) {
        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        if (sale.getStatus() != SaleStatus.VALIDATED && sale.getStatus() != SaleStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException("Seule une vente validee peut etre remboursee");
        }

        boolean returnToStock = request.getReturnToStock() == null || request.getReturnToStock();
        List<SaleRefundLine> refundLines = new ArrayList<>();
        BigDecimal totalRefund = BigDecimal.ZERO;

        SaleRefund refund = SaleRefund.builder()
                .refundNumber(generateRefundNumber())
                .sale(sale)
                .status(SaleRefundStatus.COMPLETED)
                .reason(request.getReason())
                .returnToStock(returnToStock)
                .createdBy(currentUserService.getCurrentUserEmailOrDefault())
                .lignes(refundLines)
                .build();

        if (request.getLines() == null || request.getLines().isEmpty()) {
            for (SaleLine line : sale.getLignes()) {
                refundLines.add(buildRefundLine(refund, line, line.getQuantityInput()));
                totalRefund = totalRefund.add(line.getLineTotal());
            }
        } else {
            for (SaleRefundRequest.Line req : request.getLines()) {
                SaleLine line = sale.getLignes().stream()
                        .filter(l -> l.getId().equals(req.getSaleLineId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Ligne vente non trouvee"));
                refundLines.add(buildRefundLine(refund, line, req.getQuantity()));
                BigDecimal ratio = req.getQuantity().divide(line.getQuantityInput(), 6, RoundingMode.HALF_UP);
                totalRefund = totalRefund.add(line.getLineTotal().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
            }
        }

        refund.setTotalAmount(totalRefund);
        refund.setCompletedAt(Instant.now());

        if (returnToStock) {
            for (SaleRefundLine rl : refundLines) {
                SaleLine saleLine = rl.getSaleLine();
                ledger.applyOnHandChange(
                        saleLine.getProduct().getId(),
                        saleLine.getVariant() != null ? saleLine.getVariant().getId() : null,
                        sale.getWarehouse().getId(),
                        sale.getLocation().getId(),
                        null,
                        rl.getQuantity(),
                        new StockLedgerService.MovementMeta(
                                StockMovementType.RETURN_IN,
                                "POS_REFUND",
                                refund.getRefundNumber(),
                                "Remboursement vente " + sale.getSaleNumber(),
                                currentUserService.getCurrentUserEmailOrDefault(),
                                null, null, null, null, null,
                                saleLine.getPackaging() != null ? saleLine.getPackaging().getId() : null,
                                refund.getId(),
                                null));
            }
        }

        boolean fullRefund = request.getLines() == null || request.getLines().isEmpty();
        sale.setStatus(fullRefund ? SaleStatus.REFUNDED : SaleStatus.PARTIALLY_REFUNDED);
        saleRepository.save(sale);

        SaleRefund saved = refundRepository.save(refund);
        auditService.log("SaleRefund", saved.getId(), AuditAction.CREATION,
                "Remboursement " + saved.getRefundNumber(), saved.getCreatedBy());
        return mapper.toRefundResponse(saved);
    }

    private SaleRefundLine buildRefundLine(SaleRefund refund, SaleLine line, BigDecimal quantity) {
        BigDecimal ratio = quantity.divide(line.getQuantityInput(), 6, RoundingMode.HALF_UP);
        return SaleRefundLine.builder()
                .refund(refund)
                .saleLine(line)
                .quantity(quantity)
                .refundAmount(line.getLineTotal().multiply(ratio).setScale(4, RoundingMode.HALF_UP))
                .build();
    }

    private String generateRefundNumber() {
        String prefix = "RF-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = refundRepository.countByRefundNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }
}
