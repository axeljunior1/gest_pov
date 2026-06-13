package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.PosSessionStatus;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.settings.SettingKeys;
import lombok.RequiredArgsConstructor;
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
    private final CurrentUserService currentUserService;
    private final PosMapper mapper;

    @Transactional
    public PosSessionResponse openSession(PosSessionOpenRequest request) {
        User cashier = currentUserService.requireCurrentUser();
        sessionRepository.findByCashierIdAndStatus(cashier.getId(), PosSessionStatus.OPEN)
                .ifPresent(s -> {
                    throw new BusinessException("Une session est deja ouverte pour ce caissier");
                });

        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        PosSession session = PosSession.builder()
                .sessionNumber(generateSessionNumber())
                .cashier(cashier)
                .warehouse(warehouse)
                .openingCashAmount(request.getOpeningCashAmount() != null
                        ? request.getOpeningCashAmount() : BigDecimal.ZERO)
                .status(PosSessionStatus.OPEN)
                .build();
        return mapper.toSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public PosSessionResponse getCurrentSession() {
        User cashier = currentUserService.requireCurrentUser();
        PosSession session = sessionRepository.findByCashierIdAndStatus(cashier.getId(), PosSessionStatus.OPEN)
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
        User cashier = currentUserService.requireCurrentUser();
        PosSession session = sessionRepository.findByCashierIdAndStatus(cashier.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune session ouverte"));

        long draftCount = saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()
                .filter(s -> s.getStatus() == SaleStatus.DRAFT)
                .count();
        if (draftCount > 0) {
            throw new BusinessException("Impossible de fermer : vente(s) brouillon en cours");
        }

        PosSessionReportResponse report = buildReport(session);
        BigDecimal declared = request.getClosingCashAmount() != null
                ? request.getClosingCashAmount() : BigDecimal.ZERO;

        session.setClosingCashAmount(declared);
        session.setExpectedCashAmount(report.getExpectedCashAmount());
        session.setDifferenceAmount(declared.subtract(report.getExpectedCashAmount()));
        session.setStatus(PosSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        sessionRepository.save(session);

        report.setDeclaredCashAmount(declared);
        report.setCashDifference(session.getDifferenceAmount());
        return report;
    }

    @Transactional(readOnly = true)
    public PosSessionReportResponse getSessionReport(Long sessionId) {
        PosSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvee: " + sessionId));
        return buildReport(session);
    }

    public PosSession requireOpenSession(User cashier) {
        return sessionRepository.findByCashierIdAndStatus(cashier.getId(), PosSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Aucune session de caisse ouverte"));
    }

    private PosSessionReportResponse buildReport(PosSession session) {
        List<Sale> sales = saleRepository.findByPosSessionIdOrderByCreatedAtDesc(session.getId()).stream()
                .filter(s -> s.getStatus() == SaleStatus.VALIDATED
                        || s.getStatus() == SaleStatus.PARTIALLY_REFUNDED
                        || s.getStatus() == SaleStatus.REFUNDED)
                .toList();

        BigDecimal totalRevenue = sales.stream()
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashRevenue = paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CASH);
        BigDecimal cardRevenue = paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.CARD);
        BigDecimal mobileRevenue = paymentRepository.sumBySessionAndMethod(session.getId(), PaymentMethod.MOBILE_MONEY);

        BigDecimal opening = session.getOpeningCashAmount() != null ? session.getOpeningCashAmount() : BigDecimal.ZERO;
        BigDecimal expectedCash = opening.add(cashRevenue != null ? cashRevenue : BigDecimal.ZERO);

        return PosSessionReportResponse.builder()
                .sessionId(session.getId())
                .sessionNumber(session.getSessionNumber())
                .saleCount(sales.size())
                .totalRevenue(totalRevenue)
                .cashRevenue(cashRevenue != null ? cashRevenue : BigDecimal.ZERO)
                .cardRevenue(cardRevenue != null ? cardRevenue : BigDecimal.ZERO)
                .mobileMoneyRevenue(mobileRevenue != null ? mobileRevenue : BigDecimal.ZERO)
                .refundsTotal(BigDecimal.ZERO)
                .expectedCashAmount(expectedCash)
                .build();
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
}
