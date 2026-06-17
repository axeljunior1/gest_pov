package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PosRefundService {

    private static final String PERM_VALIDATE_SENSITIVE_REFUND = "pos.return.validate";

    private final SaleRefundRepository refundRepository;
    private final RefundPaymentRepository refundPaymentRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final StockLedgerService ledger;
    private final PosSessionService sessionService;
    private final CurrentUserService currentUserService;
    private final SettingsService settingsService;
    private final PosMapper mapper;
    private final AuditService auditService;
    private final LoyaltyService loyaltyService;
    private final PasswordEncoder passwordEncoder;
    private final com.erp.products.service.stockvaluation.StockCmpValuationService cmpValuationService;

    @Transactional(readOnly = true)
    public List<SaleResponse> searchRefundableSales(String query, Integer limit) {
        int max = limit != null && limit > 0 ? Math.min(limit, 50) : 20;
        String q = query != null ? query.trim() : "";
        if (q.isEmpty()) {
            return List.of();
        }
        return saleRepository.searchRefundableSales(
                        SaleStatuses.COUNTED_FOR_REVENUE,
                        q.toLowerCase(),
                        PageRequest.of(0, max))
                .stream()
                .map(mapper::toSaleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnableSaleResponse getReturnableSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        assertRefundableStatus(sale);
        BigDecimal alreadyRefunded = nullToZero(refundRepository.sumRefundedAmountBySale(
                saleId, SaleRefundStatus.COMPLETED));
        BigDecimal paid = nullToZero(sale.getPaidAmount());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            paid = nullToZero(sale.getTotal());
        }
        return ReturnableSaleResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .paidAt(sale.getPaidAt() != null ? sale.getPaidAt() : sale.getValidatedAt())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .customerNumber(sale.getCustomer() != null ? sale.getCustomer().getCustomerNumber() : null)
                .total(sale.getTotal())
                .paidAmount(paid)
                .amountAlreadyRefunded(alreadyRefunded)
                .amountRefundable(paid.subtract(alreadyRefunded).max(BigDecimal.ZERO))
                .lines(sale.getLignes().stream().map(this::toReturnableLine).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public SaleRefundResponse getReturn(Long refundId) {
        SaleRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Retour non trouve: " + refundId));
        return mapper.toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    public ReturnReceiptResponse buildReceipt(Long refundId) {
        SaleRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Retour non trouve: " + refundId));
        if (refund.getStatus() != SaleRefundStatus.COMPLETED) {
            throw new BusinessException("Reçu disponible uniquement pour un retour valide");
        }
        var publicSettings = settingsService.getPublicSettings();
        Sale sale = refund.getSale();
        return ReturnReceiptResponse.builder()
                .returnNumber(refund.getRefundNumber())
                .originalSaleNumber(sale.getSaleNumber())
                .returnDate(refund.getValidatedAt() != null ? refund.getValidatedAt() : refund.getCompletedAt())
                .companyName(publicSettings.getCompanyName())
                .registerName(settingsService.getSetting(SettingKeys.POS_REGISTER_NAME))
                .cashierName(refund.getCashier() != null ? refund.getCashier().fullName() : null)
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .refundTotal(refund.getTotalAmount())
                .currency(publicSettings.getCurrency())
                .reason(refund.getReason())
                .lines(refund.getLignes().stream().map(l -> ReturnReceiptResponse.ReceiptLine.builder()
                        .productNom(l.getProduct() != null ? l.getProduct().getNom() : l.getSaleLine().getProduct().getNom())
                        .packagingName(l.getPackagingNameSnapshot())
                        .quantity(l.getQuantity())
                        .refundAmount(l.getRefundAmount())
                        .restock(Boolean.TRUE.equals(l.getRestock()))
                        .reason(l.getReason())
                        .build()).toList())
                .payments(refund.getPayments().stream().map(p -> ReturnReceiptResponse.ReceiptPayment.builder()
                        .method(p.getMethod())
                        .amount(p.getAmount())
                        .build()).toList())
                .build();
    }

    @Transactional
    public SaleRefundResponse createReturn(Long saleId, SaleRefundRequest request) {
        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        assertRefundableStatus(sale);

        User cashier = currentUserService.requireCurrentUser();
        PosSession session = resolveRefundSession(cashier, sale);
        boolean defaultRestock = request.getReturnToStock() == null || request.getReturnToStock();

        List<SaleRefundLine> refundLines = new ArrayList<>();
        SaleRefund refund = SaleRefund.builder()
                .refundNumber(generateRefundNumber())
                .sale(sale)
                .cashier(cashier)
                .posSession(session)
                .customer(sale.getCustomer())
                .status(SaleRefundStatus.PENDING)
                .refundStatus(RefundFulfillmentStatus.PENDING)
                .reason(request.getReason())
                .notes(request.getNotes())
                .returnToStock(defaultRestock)
                .createdBy(currentUserService.getCurrentUserEmailOrDefault())
                .lignes(refundLines)
                .build();

        BigDecimal totalRefund = buildRefundLines(refund, sale, request, defaultRestock, refundLines);
        validateRefundAmount(sale, totalRefund);

        refund.setTotalAmount(totalRefund);
        SaleRefund saved = refundRepository.save(refund);
        return mapper.toRefundResponse(saved);
    }

    /** Compatibilité : création + validation atomique (API historique). */
    @Transactional
    public SaleRefundResponse createRefund(Long saleId, SaleRefundRequest request) {
        SaleRefundResponse draft = createReturn(saleId, request);
        RefundValidateRequest validate = new RefundValidateRequest();
        validate.setManagerEmail(request.getManagerEmail());
        validate.setManagerPassword(request.getManagerPassword());
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
            validate.setPayments(request.getPayments());
        } else {
            validate.setPayments(inferRefundPayments(
                    saleRepository.findById(saleId).orElseThrow(), draft.getTotalAmount()));
        }
        return validateReturn(draft.getId(), validate);
    }

    @Transactional
    public SaleRefundResponse validateReturn(Long refundId, RefundValidateRequest request) {
        SaleRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Retour non trouve: " + refundId));
        if (refund.getStatus() != SaleRefundStatus.PENDING) {
            throw new BusinessException("Seul un retour en brouillon peut etre valide");
        }

        Sale sale = saleRepository.findByIdForUpdate(refund.getSale().getId())
                .orElseThrow();
        assertRefundableStatus(sale);
        validateRefundAmount(sale, refund.getTotalAmount());
        validateManagerIfRequired(refund.getTotalAmount(), request);

        User cashier = currentUserService.requireCurrentUser();
        PosSession session = resolveRefundSession(cashier, sale);
        refund.setCashier(cashier);
        refund.setPosSession(session);

        List<SaleRefundRequest.RefundPaymentRequest> paymentReqs = request.getPayments();
        if (paymentReqs == null || paymentReqs.isEmpty()) {
            paymentReqs = inferRefundPayments(sale, refund.getTotalAmount());
        }
        BigDecimal paymentsTotal = paymentReqs.stream()
                .map(SaleRefundRequest.RefundPaymentRequest::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paymentsTotal.compareTo(refund.getTotalAmount()) != 0) {
            throw new BusinessException("Le total des remboursements doit egaler le montant du retour");
        }

        Instant now = Instant.now();
        for (SaleRefundLine rl : refund.getLignes()) {
            if (Boolean.TRUE.equals(rl.getRestock())) {
                SaleLine saleLine = rl.getSaleLine();
                ledger.applyOnHandChange(
                        saleLine.getProduct().getId(),
                        saleLine.getVariant() != null ? saleLine.getVariant().getId() : null,
                        sale.getWarehouse().getId(),
                        sale.getLocation().getId(),
                        null,
                        rl.getQuantityInBaseUnit(),
                        new StockLedgerService.MovementMeta(
                                StockMovementType.RETURN_IN,
                                "POS_REFUND",
                                refund.getRefundNumber(),
                                "Retour vente " + sale.getSaleNumber(),
                                currentUserService.getCurrentUserEmailOrDefault(),
                                null, null, null, null, null,
                                rl.getPackaging() != null ? rl.getPackaging().getId() : null,
                                refund.getId(),
                                null));
                Long variantId = saleLine.getVariant() != null ? saleLine.getVariant().getId() : null;
                BigDecimal unitCost = saleLine.getUnitCostAtSale();
                if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                    unitCost = cmpValuationService.resolveFallbackCost(saleLine.getProduct().getId(), variantId);
                }
                cmpValuationService.recordReturn(
                        saleLine.getProduct().getId(),
                        variantId,
                        rl.getQuantityInBaseUnit(),
                        unitCost,
                        now,
                        refund.getId(),
                        "POS_REFUND");
            }
        }

        refund.getPayments().clear();
        for (SaleRefundRequest.RefundPaymentRequest pr : paymentReqs) {
            Payment original = null;
            if (pr.getOriginalPaymentId() != null) {
                original = sale.getPayments().stream()
                        .filter(p -> p.getId().equals(pr.getOriginalPaymentId()))
                        .findFirst()
                        .orElse(null);
            }
            RefundPayment rp = RefundPayment.builder()
                    .refund(refund)
                    .originalPayment(original)
                    .cashier(cashier)
                    .posSession(session)
                    .method(pr.getMethod())
                    .amount(pr.getAmount())
                    .status(RefundPaymentStatus.REFUNDED)
                    .build();
            refund.getPayments().add(rp);
        }
        refundPaymentRepository.saveAll(refund.getPayments());

        updateSaleStatusAfterReturn(sale, refund);
        refund.setStatus(SaleRefundStatus.COMPLETED);
        refund.setRefundStatus(resolveFulfillmentStatus(sale));
        refund.setValidatedAt(now);
        refund.setCompletedAt(now);
        if (request.getManagerEmail() != null && !request.getManagerEmail().isBlank()) {
            refund.setManagerValidatedBy(request.getManagerEmail().trim().toLowerCase());
            refund.setManagerValidatedAt(now);
        }

        saleRepository.save(sale);
        SaleRefund saved = refundRepository.save(refund);
        loyaltyService.processRefund(sale, saved.getTotalAmount());
        auditService.log("SaleRefund", saved.getId(), AuditAction.CREATION,
                "Retour valide " + saved.getRefundNumber(), saved.getCreatedBy());
        return mapper.toRefundResponse(saved);
    }

    private ReturnableLineResponse toReturnableLine(SaleLine line) {
        BigDecimal already = nullToZero(refundRepository.sumReturnedQuantityBySaleLine(
                line.getId(), SaleRefundStatus.COMPLETED));
        BigDecimal sold = line.getQuantityInput();
        BigDecimal returnable = sold.subtract(already).max(BigDecimal.ZERO);
        BigDecimal maxRefund = computeLineRefundAmount(line, returnable);
        return ReturnableLineResponse.builder()
                .saleLineId(line.getId())
                .productId(line.getProduct().getId())
                .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                .productNom(line.getProductNameSnapshot() != null
                        ? line.getProductNameSnapshot() : line.getProduct().getNom())
                .variantNameSnapshot(line.getVariantNameSnapshot())
                .productSku(line.getProduct().getSku())
                .packagingId(line.getPackaging() != null ? line.getPackaging().getId() : null)
                .packagingNameSnapshot(line.getPackagingNameSnapshot())
                .packagingQuantitySnapshot(line.getPackagingQuantitySnapshot())
                .quantitySold(sold)
                .quantityAlreadyReturned(already)
                .quantityReturnable(returnable)
                .unitPriceSnapshot(line.getUnitPriceSnapshot() != null ? line.getUnitPriceSnapshot() : line.getUnitPrice())
                .lineTotal(line.getLineTotal())
                .maxRefundAmount(maxRefund)
                .build();
    }

    private BigDecimal buildRefundLines(SaleRefund refund, Sale sale, SaleRefundRequest request,
                                        boolean defaultRestock, List<SaleRefundLine> refundLines) {
        BigDecimal totalRefund = BigDecimal.ZERO;
        if (request.getLines() == null || request.getLines().isEmpty()) {
            for (SaleLine line : sale.getLignes()) {
                ReturnableLineResponse rl = toReturnableLine(line);
                if (rl.getQuantityReturnable().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                SaleRefundLine srl = buildRefundLine(refund, line, rl.getQuantityReturnable(),
                        defaultRestock, request.getReason(), null);
                refundLines.add(srl);
                totalRefund = totalRefund.add(srl.getRefundAmount());
            }
            if (refundLines.isEmpty()) {
                throw new BusinessException("Aucune ligne retournable sur cette vente");
            }
            return totalRefund;
        }

        for (SaleRefundRequest.Line req : request.getLines()) {
            SaleLine line = sale.getLignes().stream()
                    .filter(l -> l.getId().equals(req.getSaleLineId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Ligne vente non trouvee"));
            ReturnableLineResponse rl = toReturnableLine(line);
            if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Quantite de retour invalide");
            }
            if (req.getQuantity().compareTo(rl.getQuantityReturnable()) > 0) {
                throw new BusinessException("Quantite retournee superieure a la quantite retournable pour "
                        + line.getProduct().getNom());
            }
            boolean restock = req.getRestock() != null ? req.getRestock() : defaultRestock;
            SaleRefundLine srl = buildRefundLine(refund, line, req.getQuantity(), restock,
                    req.getReason() != null ? req.getReason() : request.getReason(), req.getNotes());
            refundLines.add(srl);
            totalRefund = totalRefund.add(srl.getRefundAmount());
        }
        return totalRefund;
    }

    private SaleRefundLine buildRefundLine(SaleRefund refund, SaleLine line, BigDecimal quantity,
                                           boolean restock, String reason, String notes) {
        BigDecimal refundAmount = computeLineRefundAmount(line, quantity);
        BigDecimal baseQty = line.getQuantityInBaseUnit()
                .multiply(quantity)
                .divide(line.getQuantityInput(), 6, RoundingMode.HALF_UP);
        return SaleRefundLine.builder()
                .refund(refund)
                .saleLine(line)
                .product(line.getProduct())
                .packaging(line.getPackaging())
                .packagingNameSnapshot(line.getPackagingNameSnapshot())
                .packagingQuantitySnapshot(line.getPackagingQuantitySnapshot())
                .quantity(quantity)
                .quantityInBaseUnit(baseQty)
                .unitPriceSnapshot(line.getUnitPriceSnapshot() != null ? line.getUnitPriceSnapshot() : line.getUnitPrice())
                .refundAmount(refundAmount)
                .restock(restock)
                .reason(reason)
                .notes(notes)
                .build();
    }

    private BigDecimal computeLineRefundAmount(SaleLine line, BigDecimal quantity) {
        BigDecimal unitPrice = line.getUnitPriceSnapshot() != null ? line.getUnitPriceSnapshot() : line.getUnitPrice();
        if (quantity.compareTo(line.getQuantityInput()) == 0) {
            return line.getLineTotal();
        }
        return unitPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP);
    }

    private void validateRefundAmount(Sale sale, BigDecimal newRefundAmount) {
        BigDecimal already = nullToZero(refundRepository.sumRefundedAmountBySale(
                sale.getId(), SaleRefundStatus.COMPLETED));
        BigDecimal paid = nullToZero(sale.getPaidAmount());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            paid = nullToZero(sale.getTotal());
        }
        if (already.add(newRefundAmount).compareTo(paid) > 0) {
            throw new BusinessException("Montant rembourse superieur au montant paye");
        }
    }

    private void updateSaleStatusAfterReturn(Sale sale, SaleRefund refund) {
        boolean fullReturn = true;
        for (SaleLine line : sale.getLignes()) {
            BigDecimal already = nullToZero(refundRepository.sumReturnedQuantityBySaleLine(
                    line.getId(), SaleRefundStatus.COMPLETED));
            BigDecimal pending = refund.getLignes().stream()
                    .filter(rl -> rl.getSaleLine().getId().equals(line.getId()))
                    .map(SaleRefundLine::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (already.add(pending).compareTo(line.getQuantityInput()) < 0) {
                fullReturn = false;
                break;
            }
        }
        sale.setStatus(fullReturn ? SaleStatus.REFUNDED : SaleStatus.PARTIALLY_REFUNDED);
    }

    private RefundFulfillmentStatus resolveFulfillmentStatus(Sale sale) {
        return sale.getStatus() == SaleStatus.REFUNDED
                ? RefundFulfillmentStatus.REFUNDED
                : RefundFulfillmentStatus.PARTIALLY_REFUNDED;
    }

    private List<SaleRefundRequest.RefundPaymentRequest> inferRefundPayments(Sale sale, BigDecimal total) {
        if (sale.getPayments() == null || sale.getPayments().isEmpty()) {
            return List.of(paymentReq(PaymentMethod.CASH, total, null));
        }
        if (sale.getPayments().size() == 1) {
            Payment p = sale.getPayments().get(0);
            return List.of(paymentReq(p.getMethod(), total, p.getId()));
        }
        Payment primary = sale.getPayments().get(0);
        return List.of(paymentReq(primary.getMethod(), total, primary.getId()));
    }

    private PosSession resolveRefundSession(User user, Sale sale) {
        try {
            return sessionService.requireOpenSession(user, PosSessionType.CASHIER);
        } catch (BusinessException ignored) {
            // pas de session caisse ouverte pour cet utilisateur
        }
        if (sale.getPaymentSession() != null) {
            return sale.getPaymentSession();
        }
        return sessionService.requireOpenSession(user);
    }

    private static SaleRefundRequest.RefundPaymentRequest paymentReq(PaymentMethod method, BigDecimal amount, Long id) {
        SaleRefundRequest.RefundPaymentRequest r = new SaleRefundRequest.RefundPaymentRequest();
        r.setMethod(method);
        r.setAmount(amount);
        r.setOriginalPaymentId(id);
        return r;
    }

    private void assertRefundableStatus(Sale sale) {
        if (!SaleStatuses.isPaid(sale.getStatus()) && sale.getStatus() != SaleStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException("Seule une vente payee peut faire l'objet d'un retour");
        }
    }

    private void validateManagerIfRequired(BigDecimal amount, RefundValidateRequest request) {
        BigDecimal threshold = settingsService.getDecimal(SettingKeys.POS_REQUIRE_MANAGER_APPROVAL_ABOVE_REFUND_AMOUNT);
        if (threshold == null || amount.compareTo(threshold) <= 0) {
            return;
        }
        if (request.getManagerEmail() == null || request.getManagerEmail().isBlank()
                || request.getManagerPassword() == null || request.getManagerPassword().isBlank()) {
            throw new BusinessException("Validation manager obligatoire pour ce montant de remboursement");
        }
        User manager = userRepository.findByEmailIgnoreCase(request.getManagerEmail().trim())
                .orElseThrow(() -> new BusinessException("Manager introuvable"));
        if (!passwordEncoder.matches(request.getManagerPassword(), manager.getPasswordHash())) {
            throw new BusinessException("Identifiants manager invalides");
        }
        if (!userHasPermission(manager, PERM_VALIDATE_SENSITIVE_REFUND)) {
            throw new BusinessException("Cet utilisateur ne peut pas valider ce remboursement");
        }
    }

    private boolean userHasPermission(User user, String permission) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> permission.equals(p.getCode()))
                || user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
    }

    private String generateRefundNumber() {
        String prefix = "RF-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = refundRepository.countByRefundNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
