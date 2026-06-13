package com.erp.products.service;



import com.erp.products.domain.entity.*;

import com.erp.products.domain.enums.PaymentMethod;

import com.erp.products.domain.enums.PosSessionStatus;

import com.erp.products.domain.enums.PosSessionType;

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
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.time.Instant;

import java.time.LocalDate;

import java.time.format.DateTimeFormatter;

import java.util.List;



@Service

@RequiredArgsConstructor

public class PosSessionService {



    private final PosSessionRepository sessionRepository;

    private final SaleRepository saleRepository;

    private final PaymentRepository paymentRepository;

    private final WarehouseRepository warehouseRepository;

    private final SettingsService settingsService;

    private final PosConfigService posConfigService;

    private final CurrentUserService currentUserService;

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



    @Transactional

    public PosSessionReportResponse closeSession(PosSessionCloseRequest request) {

        User user = currentUserService.requireCurrentUser();

        PosSession session = sessionRepository.findByCashierIdAndStatus(user.getId(), PosSessionStatus.OPEN)

                .orElseThrow(() -> new ResourceNotFoundException("Aucune session ouverte"));



        boolean forceCancel = Boolean.TRUE.equals(request.getCancelPendingDrafts());

        resolveDraftSalesBeforeClose(session.getId(), forceCancel);



        PosSessionReportResponse report = buildReport(session);



        if (session.getSessionType() == PosSessionType.CASHIER) {

            BigDecimal declared = request.getClosingCashAmount() != null

                    ? request.getClosingCashAmount() : BigDecimal.ZERO;

            session.setClosingCashAmount(declared);

            session.setExpectedCashAmount(report.getExpectedCashAmount());

            session.setDifferenceAmount(declared.subtract(report.getExpectedCashAmount()));

            report.setDeclaredCashAmount(declared);

            report.setCashDifference(session.getDifferenceAmount());

        } else {

            session.setClosingCashAmount(BigDecimal.ZERO);

            session.setExpectedCashAmount(BigDecimal.ZERO);

            session.setDifferenceAmount(BigDecimal.ZERO);

            report.setDeclaredCashAmount(BigDecimal.ZERO);

            report.setCashDifference(BigDecimal.ZERO);

        }



        session.setStatus(PosSessionStatus.CLOSED);

        session.setClosedAt(Instant.now());

        sessionRepository.save(session);

        return report;

    }



    @Transactional(readOnly = true)

    public PosSessionReportResponse getSessionReport(Long sessionId) {

        PosSession session = sessionRepository.findById(sessionId)

                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvee: " + sessionId));

        return buildReport(session);

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

        return request.getOpeningCashAmount() != null

                ? request.getOpeningCashAmount() : BigDecimal.ZERO;

    }



    private PosSessionReportResponse buildReport(PosSession session) {

        List<Sale> sales;

        if (session.getSessionType() == PosSessionType.CASHIER) {

            sales = saleRepository.findByPaymentSessionIdOrderByCreatedAtDesc(session.getId()).stream()

                    .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()))

                    .toList();

            if (sales.isEmpty()) {

                sales = saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()

                        .filter(s -> s.getPaymentSession() == null

                                && (SaleStatuses.countsForRevenue(s.getStatus())))

                        .toList();

            }

        } else {

            sales = saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()

                    .filter(s -> SaleStatuses.countsForRevenue(s.getStatus())

                            || s.getStatus() == SaleStatus.PENDING_PAYMENT)

                    .toList();

        }



        BigDecimal totalRevenue = sales.stream()

                .filter(s -> SaleStatuses.countsForRevenue(s.getStatus()))

                .map(Sale::getTotal)

                .reduce(BigDecimal.ZERO, BigDecimal::add);



        BigDecimal cashRevenue = BigDecimal.ZERO;

        BigDecimal cardRevenue = BigDecimal.ZERO;

        BigDecimal mobileRevenue = BigDecimal.ZERO;

        if (session.getSessionType() == PosSessionType.CASHIER) {

            cashRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CASH));

            cardRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CARD));

            mobileRevenue = nullToZero(paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.MOBILE_MONEY));

        }



        BigDecimal opening = session.getOpeningCashAmount() != null ? session.getOpeningCashAmount() : BigDecimal.ZERO;

        BigDecimal expectedCash = opening.add(cashRevenue);



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

                .refundsTotal(BigDecimal.ZERO)

                .expectedCashAmount(expectedCash)

                .build();

    }



    private static BigDecimal nullToZero(BigDecimal value) {

        return value != null ? value : BigDecimal.ZERO;

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

