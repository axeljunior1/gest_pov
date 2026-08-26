package com.erp.products.service;

import com.erp.products.domain.entity.Payment;
import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.entity.SaleEvent;
import com.erp.products.domain.entity.User;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.dto.analytics.AnalyticsFilterRequest;
import com.erp.products.dto.analytics.CancellationActorStat;
import com.erp.products.dto.analytics.CancellationAnalyticsResponse;
import com.erp.products.dto.analytics.CancellationReasonStat;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.AnalyticsRepository;
import com.erp.products.repository.SaleCancellationRepository;
import com.erp.products.repository.SaleEventRepository;
import com.erp.products.repository.SaleRepository;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.analytics.AnalyticsConstants;
import com.erp.products.service.analytics.AnalyticsFilterService;
import com.erp.products.service.analytics.ResolvedAnalyticsFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleCancellationService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final SaleRepository saleRepository;
    private final SaleCancellationRepository cancellationRepository;
    private final SaleEventRepository saleEventRepository;
    private final SaleEventService saleEventService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final PosMapper mapper;
    private final AnalyticsFilterService filterService;
    private final AnalyticsRepository analyticsRepository;

    @Transactional
    public SaleResponse cancel(Sale sale, CancelSaleRequest request, User cancelledBy, boolean sessionAuto) {
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Vente deja annulee");
        }
        if (SaleStatuses.isPaid(sale.getStatus())
                || sale.getStatus() == SaleStatus.REFUNDED
                || sale.getStatus() == SaleStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException("Une vente payee doit etre remboursee, pas annulee");
        }

        User actor = cancelledBy != null ? cancelledBy : currentUserService.requireCurrentUser();
        SaleCancellationReason reason = sessionAuto
                ? SaleCancellationReason.SESSION_AUTO
                : (request != null && request.getReason() != null
                ? request.getReason() : SaleCancellationReason.OTHER);
        String comment = request != null ? request.getComment() : null;

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(Instant.now());
        sale.setCancelledBy(actor);
        sale.setCancellationReason(reason);
        sale.setCancellationComment(comment);
        sale.setUpdatedBy(actor);

        Sale saved = saleRepository.save(sale);

        String reasonLabel = reason.getLabel();
        String eventDetails = comment != null && !comment.isBlank()
                ? reasonLabel + " — " + comment.trim() : reasonLabel;
        saleEventService.record(saved, SaleEventType.CANCELLED, "Annulation", eventDetails, actor);
        auditService.log("Sale", saved.getId(), AuditAction.CHANGEMENT_STATUT,
                "Vente annulee " + saved.getSaleNumber() + " — " + eventDetails,
                actor.getEmail());

        return mapper.toSaleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CancelledSaleSummaryResponse> list(CancelledSaleFilterRequest request) {
        ResolvedAnalyticsFilter period = filterService.resolve(toAnalyticsFilter(request));
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 50;

        return cancellationRepository.findCancelled(
                        period.getFrom(), period.getTo(),
                        request.getSellerId(), request.getCashierId(), request.getCustomerId(),
                        request.getReason(), request.getAmountMin(), request.getAmountMax(),
                        page, size)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CancelledSaleDetailResponse getDetail(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        if (sale.getStatus() != SaleStatus.CANCELLED) {
            throw new BusinessException("Cette vente n'est pas annulee");
        }

        List<SaleEvent> events = saleEventRepository.findBySaleIdOrderByOccurredAtAscIdAsc(saleId);

        return CancelledSaleDetailResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .status(sale.getStatus())
                .createdAt(sale.getCreatedAt())
                .cancelledAt(sale.getCancelledAt())
                .sellerId(sale.getSeller().getId())
                .sellerName(sale.getSeller().fullName())
                .cashierId(sale.getCashier().getId())
                .cashierName(sale.getCashier().fullName())
                .customerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null)
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .cancellationReason(sale.getCancellationReason())
                .cancellationReasonLabel(label(sale.getCancellationReason()))
                .cancellationComment(sale.getCancellationComment())
                .total(sale.getTotal())
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .taxTotal(sale.getTaxTotal())
                .lignes(sale.getLignes().stream().map(mapper::toLineResponse).toList())
                .paymentInfo(buildPaymentInfo(sale))
                .timeline(buildTimeline(sale, events))
                .audit(buildAuditInfo(sale))
                .build();
    }

    @Transactional(readOnly = true)
    public CancellationAnalyticsResponse getAnalytics(AnalyticsFilterRequest request) {
        ResolvedAnalyticsFilter filter = filterService.resolve(request);

        long count = analyticsRepository.countCancelledSales(filter.getFrom(), filter.getTo(), filter);
        BigDecimal amount = analyticsRepository.sumCancelledAmount(filter.getFrom(), filter.getTo(), filter);
        long prevCount = analyticsRepository.countCancelledSales(filter.getCompareFrom(), filter.getCompareTo(), filter);
        BigDecimal prevAmount = analyticsRepository.sumCancelledAmount(filter.getCompareFrom(), filter.getCompareTo(), filter);

        return CancellationAnalyticsResponse.builder()
                .cancelledCount(count)
                .cancelledAmountTotal(amount)
                .cancelledCountPeriod(filterService.compare(count, prevCount))
                .cancelledAmountPeriod(filterService.compare(amount, prevAmount))
                .topReasons(mapReasonStats(analyticsRepository.findTopCancellationReasons(
                        filter.getFrom(), filter.getTo(), filter, 5)))
                .topSellers(mapActorStats(analyticsRepository.findTopCancellationSellers(
                        filter.getFrom(), filter.getTo(), filter, 5)))
                .topCashiers(mapActorStats(analyticsRepository.findTopCancellationCashiers(
                        filter.getFrom(), filter.getTo(), filter, 5)))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SaleCancellationReasonOption> listReasons() {
        return Arrays.stream(SaleCancellationReason.values())
                .filter(r -> r != SaleCancellationReason.SESSION_AUTO)
                .map(r -> new SaleCancellationReasonOption(r.name(), r.getLabel()))
                .toList();
    }

    private List<CancellationReasonStat> mapReasonStats(List<Object[]> rows) {
        return rows.stream().map(row -> CancellationReasonStat.builder()
                .reason((SaleCancellationReason) row[0])
                .reasonLabel(((SaleCancellationReason) row[0]).getLabel())
                .count(AnalyticsConstants.longValue(row[1]))
                .amount(AnalyticsConstants.decimalValue(row[2]))
                .build()).toList();
    }

    private List<CancellationActorStat> mapActorStats(List<Object[]> rows) {
        return rows.stream().map(row -> CancellationActorStat.builder()
                .userId(((Number) row[0]).longValue())
                .userName((row[1] + " " + row[2]).trim())
                .count(AnalyticsConstants.longValue(row[3]))
                .amount(AnalyticsConstants.decimalValue(row[4]))
                .build()).toList();
    }

    private List<SaleEventResponse> buildTimeline(Sale sale, List<SaleEvent> events) {
        if (!events.isEmpty()) {
            return events.stream().map(this::toEventResponse).toList();
        }
        List<SaleEventResponse> synthetic = new java.util.ArrayList<>();
        User created = sale.getCreatedBy() != null ? sale.getCreatedBy() : sale.getSeller();
        if (sale.getCreatedAt() != null) {
            synthetic.add(SaleEventResponse.builder()
                    .eventType(SaleEventType.CREATED)
                    .eventTypeLabel(SaleEventType.CREATED.getLabel())
                    .description("Création vente")
                    .actorName(created != null ? created.fullName() : null)
                    .occurredAt(sale.getCreatedAt())
                    .build());
        }
        if (sale.getSubmittedAt() != null) {
            synthetic.add(SaleEventResponse.builder()
                    .eventType(SaleEventType.SENT_TO_CASHIER)
                    .eventTypeLabel(SaleEventType.SENT_TO_CASHIER.getLabel())
                    .description("Envoi caisse")
                    .actorName(sale.getSeller().fullName())
                    .occurredAt(sale.getSubmittedAt())
                    .build());
        }
        if (sale.getCancelledAt() != null) {
            synthetic.add(SaleEventResponse.builder()
                    .eventType(SaleEventType.CANCELLED)
                    .eventTypeLabel(SaleEventType.CANCELLED.getLabel())
                    .description("Annulation")
                    .details(label(sale.getCancellationReason()))
                    .actorName(sale.getCancelledBy() != null ? sale.getCancelledBy().fullName() : null)
                    .occurredAt(sale.getCancelledAt())
                    .build());
        }
        return synthetic;
    }

    private CancelledSaleSummaryResponse toSummary(Sale sale) {
        return CancelledSaleSummaryResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .createdAt(sale.getCreatedAt())
                .cancelledAt(sale.getCancelledAt())
                .sellerName(sale.getSeller().fullName())
                .cashierName(sale.getCashier().fullName())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .total(sale.getTotal())
                .cancellationReason(sale.getCancellationReason())
                .cancellationReasonLabel(label(sale.getCancellationReason()))
                .status(sale.getStatus())
                .build();
    }

    private SaleEventResponse toEventResponse(SaleEvent event) {
        return SaleEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .eventTypeLabel(event.getEventType().getLabel())
                .description(event.getDescription())
                .details(event.getDetails())
                .actorName(event.getActorName())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    private CancelledSalePaymentInfo buildPaymentInfo(Sale sale) {
        boolean started = !sale.getPayments().isEmpty()
                || (sale.getPaidAmount() != null && sale.getPaidAmount().compareTo(BigDecimal.ZERO) > 0);
        boolean validated = SaleStatuses.isPaid(sale.getStatus()) || sale.getValidatedAt() != null;
        Payment first = sale.getPayments().isEmpty() ? null : sale.getPayments().get(0);
        return CancelledSalePaymentInfo.builder()
                .paymentStarted(started)
                .paymentValidated(validated)
                .paymentMethod(first != null ? first.getMethod().name() : null)
                .paymentMethodLabel(first != null ? paymentLabel(first.getMethod()) : null)
                .paidAmount(sale.getPaidAmount())
                .build();
    }

    private CancelledSaleAuditInfo buildAuditInfo(Sale sale) {
        User created = sale.getCreatedBy() != null ? sale.getCreatedBy() : sale.getSeller();
        User updated = sale.getUpdatedBy() != null ? sale.getUpdatedBy() : created;
        return CancelledSaleAuditInfo.builder()
                .createdByName(created != null ? created.fullName() : null)
                .createdAt(sale.getCreatedAt())
                .updatedByName(updated != null ? updated.fullName() : null)
                .lastUpdatedAt(sale.getCancelledAt() != null ? sale.getCancelledAt() : sale.getCreatedAt())
                .cancelledByName(sale.getCancelledBy() != null ? sale.getCancelledBy().fullName() : null)
                .cancelledAt(sale.getCancelledAt())
                .build();
    }

    private static String label(SaleCancellationReason reason) {
        return reason != null ? reason.getLabel() : null;
    }

    private static String paymentLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Espèces";
            case CARD -> "Carte";
            case MOBILE_MONEY -> "Mobile money";
            case BANK_TRANSFER -> "Virement";
            case OTHER -> "Autre";
        };
    }

    private AnalyticsFilterRequest toAnalyticsFilter(CancelledSaleFilterRequest request) {
        AnalyticsFilterRequest f = new AnalyticsFilterRequest();
        f.setPeriod(request.getPeriod() != null ? request.getPeriod() : "THIS_MONTH");
        return f;
    }

    public record SaleCancellationReasonOption(String code, String label) {}
}
