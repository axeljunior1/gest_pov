package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.CashDifferenceReason;
import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.PosSessionStatus;
import com.erp.products.domain.enums.PosSessionType;
import com.erp.products.domain.enums.SaleRefundStatus;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.domain.enums.SaleStatuses;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PosSessionService {

    private static final String PERM_VALIDATE_CASH_DIFFERENCE = "pos.session.validate_cash_difference";

    private final PosSessionRepository sessionRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final SaleRefundRepository refundRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final SettingsService settingsService;
    private final PosConfigService posConfigService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final PosMapper mapper;

    @Transactional
    public PosSessionResponse openSession(PosSessionOpenRequest request) {
        User user = currentUserService.requireCurrentUser();
        sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)
                .ifPresent(s -> {
                    throw new BusinessException("Une session est deja ouverte pour cet utilisateur");
                });

        PosSessionType sessionType = resolveSessionType(request);
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        BigDecimal openingCash = resolveOpeningCash(request, sessionType);

        PosSession session = PosSession.builder()
                .sessionNumber(generateSessionNumber())
                .cashier(user)
                .warehouse(warehouse)
                .openingCashAmount(openingCash)
                .sessionType(sessionType)
                .status(PosSessionStatus.OPEN)
                .build();
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public PosSessionResponse getCurrentSession() {
        User user = currentUserService.requireCurrentUser();
        PosSession session = sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session ouverte"));
        return mapper.toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public PosSessionResponse getCurrentSessionOrNull() {
        return currentUserService.getCurrentUser()
                .flatMap(u -> sessionRepository.findByCashierIdAndStatus(u.getId(), PosSessionStatus.OPEN))
                .map(mapper::toSessionResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PosSessionReportResponse getClosePreview() {
        User user = currentUserService.requireCurrentUser();
        PosSession session = sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session ouverte"));
        if (session.getSessionType() != PosSessionType.CASHIER) {
            throw new BusinessException("Apercu de cloture reserve aux sessions caisse (encaissement)");
        }
        return enrichReport(session, buildReport(session));
    }

    @Transactional
    public PosSessionReportResponse closeSession(PosSessionCloseRequest request) {
        User user = currentUserService.requireCurrentUser();
        PosSession session = sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session ouverte"));

        boolean forceCancel = Boolean.TRUE.equals(request.getCancelPendingDrafts());
        resolveDraftSalesBeforeClose(session.getId(), forceCancel);

        PosSessionReportResponse report = enrichReport(session, buildReport(session));

        if (session.getSessionType() == PosSessionType.CASHIER) {
            applyCashierClose(session, request, report, user);
        } else {
            session.setClosingCashAmount(BigDecimal.ZERO);
            session.setExpectedCashAmount(BigDecimal.ZERO);
            session.setDifferenceAmount(BigDecimal.ZERO);
            report.setDeclaredCashAmount(BigDecimal.ZERO);
            report.setCashDifference(BigDecimal.ZERO);
            report.setBalanced(true);
            report.setDifferenceSeverity("BALANCED");
        }

        session.setStatus(PosSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session.setClosedBy(user.getEmail());
        sessionRepository.save(session);

        report.setClosedAt(session.getClosedAt());
        report.setClosedBy(session.getClosedBy());
        return report;
    }

    @Transactional(readOnly = true)
    public PosSessionReportResponse getSessionReport(Long sessionId) {
        PosSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvee: " + sessionId));
        PosSessionReportResponse report = enrichReport(session, buildReport(session));
        if (session.getStatus() == PosSessionStatus.CLOSED) {
            report.setDeclaredCashAmount(session.getClosingCashAmount());
            report.setCashDifference(session.getDifferenceAmount());
            report.setDifferenceReason(session.getDifferenceReason());
            report.setDifferenceComment(session.getDifferenceComment());
            report.setManagerValidatedBy(session.getManagerValidatedBy());
            report.setManagerValidatedAt(session.getManagerValidatedAt());
            report.setClosedBy(session.getClosedBy());
            report.setClosedAt(session.getClosedAt());
            applyDifferenceMeta(report, session.getDifferenceAmount());
            if (session.getDifferenceReason() != null) {
                try {
                    report.setDifferenceReasonLabel(CashDifferenceReason.valueOf(session.getDifferenceReason()).getLabel());
                } catch (IllegalArgumentException ignored) {
                    report.setDifferenceReasonLabel(session.getDifferenceReason());
                }
            }
        }
        return report;
    }

    public PosSession requireOpenSession(User user) {
        return sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Aucune session ouverte"));
    }

    public PosSession requireOpenSession(User user, PosSessionType sessionType) {
        return sessionRepository.findByCashierIdAndStatusAndSessionType(
                        user.getId(), PosSessionStatus.OPEN, sessionType)
                .orElseThrow(() -> new BusinessException(
                        "Aucune session " + sessionType + " ouverte pour cet utilisateur"));
    }

    public PosSession requireSessionForSaleCreation(User user) {
        PosSessionType required = posConfigService.requiredSessionTypeForSaleCreation();
        var typed = sessionRepository.findByCashierIdAndStatusAndSessionType(
                user.getId(), PosSessionStatus.OPEN, required);
        if (typed.isPresent()) {
            return typed.get();
        }
        if (posConfigService.isSellerCollectsPayment()) {
            return requireOpenSession(user);
        }
        var any = sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN);
        if (any.isPresent()) {
            throw new BusinessException(
                    "Session " + any.get().getSessionType() + " ouverte : fermez-la puis ouvrez une session "
                    + required + " pour preparer des ventes");
        }
        throw new BusinessException(
                "Aucune session " + required + " ouverte — ouvrez une session vente pour ajouter des produits");
    }

    private void applyCashierClose(PosSession session, PosSessionCloseRequest request,
                                   PosSessionReportResponse report, User closingUser) {
        if (request.getClosingCashAmount() == null) {
            throw new BusinessException("Montant cash reellement present en caisse obligatoire");
        }

        BigDecimal declared = request.getClosingCashAmount();
        BigDecimal expected = report.getExpectedCashAmount();
        BigDecimal difference = declared.subtract(expected);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            if (request.getDifferenceReason() == null || request.getDifferenceReason().isBlank()) {
                throw new BusinessException("Motif de l'ecart obligatoire");
            }
            CashDifferenceReason reason = CashDifferenceReason.parse(request.getDifferenceReason());
            if (reason == null) {
                throw new BusinessException("Motif d'ecart invalide");
            }
            session.setDifferenceReason(reason.name());
            session.setDifferenceComment(trimComment(request.getDifferenceComment()));

            if (settingsService.getBoolean(SettingKeys.POS_REQUIRE_MANAGER_VALIDATION_FOR_CASH_DIFFERENCE)) {
                validateManagerApproval(request, closingUser);
                session.setManagerValidatedBy(request.getManagerEmail().trim().toLowerCase());
                session.setManagerValidatedAt(Instant.now());
                report.setManagerValidatedBy(session.getManagerValidatedBy());
                report.setManagerValidatedAt(session.getManagerValidatedAt());
            }
        } else {
            session.setDifferenceReason(null);
            session.setDifferenceComment(null);
        }

        session.setClosingCashAmount(declared);
        session.setExpectedCashAmount(expected);
        session.setDifferenceAmount(difference);

        report.setDeclaredCashAmount(declared);
        report.setCashDifference(difference);
        report.setDifferenceReason(session.getDifferenceReason());
        report.setDifferenceComment(session.getDifferenceComment());
        if (session.getDifferenceReason() != null) {
            report.setDifferenceReasonLabel(CashDifferenceReason.valueOf(session.getDifferenceReason()).getLabel());
        }
        applyDifferenceMeta(report, difference);
    }

    private void validateManagerApproval(PosSessionCloseRequest request, User closingUser) {
        if (request.getManagerEmail() == null || request.getManagerEmail().isBlank()
                || request.getManagerPassword() == null || request.getManagerPassword().isBlank()) {
            throw new BusinessException("Validation manager obligatoire : identifiants manager requis");
        }
        String email = request.getManagerEmail().trim().toLowerCase();
        if (email.equalsIgnoreCase(closingUser.getEmail())) {
            throw new BusinessException("La validation manager doit etre effectuee par un autre utilisateur");
        }
        User manager = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new BusinessException("Identifiants manager invalides"));
        if (!passwordEncoder.matches(request.getManagerPassword(), manager.getPasswordHash())) {
            throw new BusinessException("Identifiants manager invalides");
        }
        if (!userHasPermission(manager, PERM_VALIDATE_CASH_DIFFERENCE)) {
            throw new BusinessException("Cet utilisateur ne peut pas valider un ecart de caisse");
        }
    }

    private boolean userHasPermission(User user, String permission) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> permission.equals(p.getCode()))
                || user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
    }

    private PosSessionReportResponse enrichReport(PosSession session, PosSessionReportResponse report) {
        report.setCashierName(session.getCashier().fullName());
        report.setOpenedAt(session.getOpenedAt());
        report.setOpeningCashAmount(nullToZero(session.getOpeningCashAmount()));
        report.setRequireManagerValidationForDifference(
                settingsService.getBoolean(SettingKeys.POS_REQUIRE_MANAGER_VALIDATION_FOR_CASH_DIFFERENCE));
        Integer alertThreshold = settingsService.getInteger(SettingKeys.POS_ALERT_CASH_DIFFERENCE_THRESHOLD);
        report.setAlertCashDifferenceThreshold(alertThreshold != null ? alertThreshold : 20);
        report.setDifferenceReasonOptions(Arrays.stream(CashDifferenceReason.values())
                .map(r -> CashDifferenceReasonOption.builder().code(r.name()).label(r.getLabel()).build())
                .toList());
        if (session.getSessionType() == PosSessionType.CASHIER && report.getCashDifference() == null) {
            applyDifferenceMeta(report, BigDecimal.ZERO);
        }
        return report;
    }

    private void applyDifferenceMeta(PosSessionReportResponse report, BigDecimal difference) {
        if (difference == null) {
            difference = BigDecimal.ZERO;
        }
        boolean balanced = difference.compareTo(BigDecimal.ZERO) == 0;
        report.setBalanced(balanced);
        report.setCashDifference(difference);
        if (balanced) {
            report.setDifferenceSeverity("BALANCED");
            return;
        }
        Integer thresholdSetting = settingsService.getInteger(SettingKeys.POS_ALERT_CASH_DIFFERENCE_THRESHOLD);
        int threshold = thresholdSetting != null ? thresholdSetting : 20;
        BigDecimal abs = difference.abs();
        report.setDifferenceSeverity(abs.compareTo(BigDecimal.valueOf(threshold)) <= 0 ? "MINOR" : "MAJOR");
    }

    private PosSessionType resolveSessionType(PosSessionOpenRequest request) {
        PosSessionType requested = request.getSessionType() != null
                ? request.getSessionType()
                : resolveDefaultSessionTypeForCurrentUser();
        enforceSessionTypeAllowed(requested);
        return requested;
    }

    private PosSessionType resolveDefaultSessionTypeForCurrentUser() {
        if (!posConfigService.isCentralCashier()) {
            return PosSessionType.CASHIER;
        }
        if (canCollectPayment() && !canPrepareSales()) {
            return PosSessionType.CASHIER;
        }
        return PosSessionType.SALES;
    }

    private void enforceSessionTypeAllowed(PosSessionType sessionType) {
        if (!posConfigService.isCentralCashier()) {
            return;
        }
        if (sessionType == PosSessionType.CASHIER && !canCollectPayment()) {
            throw new BusinessException("Seul un caissier peut ouvrir une session caisse");
        }
        if (sessionType == PosSessionType.SALES && !canPrepareSales()) {
            throw new BusinessException("Seuls les vendeurs peuvent ouvrir une session vente");
        }
    }

    private boolean canCollectPayment() {
        return hasAnyAuthority("pos.payment.collect", "pos.sale.validate");
    }

    private boolean canPrepareSales() {
        return hasAnyAuthority("pos.sale.send_to_payment", "pos.sale.prepare", "pos.sale.create");
    }

    private boolean hasAnyAuthority(String... permissions) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String code = authority.getAuthority();
            if ("ROLE_SUPER_ADMIN".equals(code)) {
                return true;
            }
            for (String permission : permissions) {
                if (permission.equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private BigDecimal resolveOpeningCash(PosSessionOpenRequest request, PosSessionType sessionType) {
        if (sessionType == PosSessionType.SALES) {
            return BigDecimal.ZERO;
        }
        if (request.getOpeningCashAmount() == null) {
            throw new BusinessException("Fond de caisse initial obligatoire pour ouvrir la caisse");
        }
        if (request.getOpeningCashAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Fond de caisse initial invalide");
        }
        return request.getOpeningCashAmount();
    }

    private PosSessionReportResponse buildReport(PosSession session) {
        List<Sale> sales = resolveSalesForReport(session);

        BigDecimal totalRevenue = sales.stream()
                .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()))
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cardRevenue = BigDecimal.ZERO;
        BigDecimal mobileRevenue = BigDecimal.ZERO;
        BigDecimal bankRevenue = BigDecimal.ZERO;
        BigDecimal cashRefundTotal = BigDecimal.ZERO;
        BigDecimal refundsTotal = BigDecimal.ZERO;

        if (session.getSessionType() == PosSessionType.CASHIER) {
            cashRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CASH));
            cardRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CARD));
            mobileRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.MOBILE_MONEY));
            bankRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.BANK_TRANSFER));
            cashRefundTotal = nullToZero(refundRepository.sumCashRefundsBySession(session.getId(), SaleRefundStatus.COMPLETED));
            refundsTotal = cashRefundTotal;
        }

        BigDecimal opening = nullToZero(session.getOpeningCashAmount());
        BigDecimal expectedCash = opening.add(cashRevenue).subtract(cashRefundTotal);

        long validatedCount = sales.stream()
                .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()))
                .count();

        return PosSessionReportResponse.builder()
                .sessionId(session.getId())
                .sessionNumber(session.getSessionNumber())
                .saleCount((int) validatedCount)
                .totalRevenue(totalRevenue)
                .cashRevenue(cashRevenue)
                .cardRevenue(cardRevenue)
                .mobileMoneyRevenue(mobileRevenue)
                .bankTransferRevenue(bankRevenue)
                .refundsTotal(refundsTotal)
                .cashRefundTotal(cashRefundTotal)
                .openingCashAmount(opening)
                .expectedCashAmount(expectedCash)
                .build();
    }

    private List<Sale> resolveSalesForReport(PosSession session) {
        if (session.getSessionType() == PosSessionType.CASHIER) {
            List<Sale> sales = saleRepository.findByPaymentSessionIdOrderByCreatedAtDesc(session.getId()).stream()
                    .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()))
                    .toList();
            if (sales.isEmpty()) {
                return saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()
                        .filter(s -> s.getPaymentSession() == null && SaleStatuses.countsForRevenue(s.getStatus()))
                        .toList();
            }
            return sales;
        }
        return saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()
                .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()) || s.getStatus() == SaleStatus.PENDING_PAYMENT)
                .toList();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String trimComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Warehouse resolveWarehouse(Long warehouseId) {
        if (warehouseId != null) {
            return warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entrepot non trouve: " + warehouseId));
        }
        String code = settingsService.getSetting(SettingKeys.POS_DEFAULT_WAREHOUSE_CODE);
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Entrepot POS par defaut introuvable: " + code));
    }

    private String generateSessionNumber() {
        String prefix = "SES-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long seq = sessionRepository.countBySessionNumberStartingWith(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private void resolveDraftSalesBeforeClose(Long sessionId, boolean forceCancelAll) {
        List<Sale> drafts = saleRepository.findByPosSessionIdAndStatusOrderByCreatedAtDesc(
                sessionId, SaleStatus.DRAFT);

        if (forceCancelAll) {
            drafts.forEach(this::cancelDraftSale);
            return;
        }

        for (Sale draft : drafts) {
            if (draft.getLignes() == null || draft.getLignes().isEmpty()) {
                cancelDraftSale(draft);
            }
        }

        long remaining = saleRepository.findByPosSessionIdAndStatusOrderByCreatedAtDesc(
                sessionId, SaleStatus.DRAFT).size();
        if (remaining > 0) {
            throw new BusinessException(
                    "Impossible de fermer : " + remaining + " vente(s) brouillon avec articles. "
                            + "Validez, mettez en attente, annulez-les ou confirmez leur annulation a la fermeture.");
        }
    }

    private void cancelDraftSale(Sale sale) {
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(Instant.now());
        saleRepository.save(sale);
    }
}
