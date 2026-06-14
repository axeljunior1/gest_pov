package com.erp.products.service;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.entity.SaleEvent;
import com.erp.products.domain.entity.SaleRefund;
import com.erp.products.domain.entity.User;
import com.erp.products.domain.enums.ExportFormat;
import com.erp.products.domain.enums.SaleRefundStatus;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.dto.*;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.PosMapper;
import com.erp.products.repository.SaleEventRepository;
import com.erp.products.repository.SaleRefundRepository;
import com.erp.products.repository.SaleRepository;
import com.erp.products.security.CurrentUserService;
import com.erp.products.security.PermissionEvaluator;
import com.erp.products.util.TabularFileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SalesBrowseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 5000;

    private final SaleRepository saleRepository;
    private final SaleRefundRepository refundRepository;
    private final SaleEventRepository saleEventRepository;
    private final PosMapper mapper;
    private final PosRefundService refundService;
    private final CurrentUserService currentUserService;
    private final PermissionEvaluator permissionChecker;

    @Transactional(readOnly = true)
    public BrowsePageResponse<SaleSummaryResponse> listSales(SaleBrowseFilterRequest request) {
        SaleBrowseFilters filters = resolveSaleFilters(request);
        List<Sale> sales = saleRepository.searchBrowseSales(
                filters.filterStatus(),
                filters.status(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId(),
                PageRequest.of(filters.page(), filters.size()));

        long total = saleRepository.countBrowseSales(
                filters.filterStatus(),
                filters.status(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId());

        Map<Long, RefundAggregate> aggregates = loadRefundAggregates(
                sales.stream().map(Sale::getId).toList());

        List<SaleSummaryResponse> items = sales.stream()
                .map(sale -> toSummary(sale, aggregates.get(sale.getId())))
                .toList();

        return toPage(items, total, filters.page(), filters.size());
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesCsv(SaleBrowseFilterRequest request) {
        SaleBrowseFilters filters = resolveSaleFilters(request);
        List<Sale> sales = saleRepository.searchBrowseSales(
                filters.filterStatus(),
                filters.status(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId(),
                PageRequest.of(0, EXPORT_LIMIT));

        Map<Long, RefundAggregate> aggregates = loadRefundAggregates(
                sales.stream().map(Sale::getId).toList());

        List<String> headers = List.of(
                "id", "saleNumber", "status", "createdAt", "paidAt", "validatedAt",
                "customerName", "sellerName", "cashierName", "total", "paidAmount",
                "refundCount", "totalRefunded");

        List<List<String>> rows = sales.stream()
                .map(sale -> {
                    RefundAggregate agg = aggregates.getOrDefault(sale.getId(), RefundAggregate.EMPTY);
                    return List.of(
                            str(sale.getId()),
                            str(sale.getSaleNumber()),
                            str(sale.getStatus()),
                            str(sale.getCreatedAt()),
                            str(sale.getPaidAt()),
                            str(sale.getValidatedAt()),
                            str(sale.getCustomer() != null ? sale.getCustomer().fullName() : null),
                            str(sale.getSeller() != null ? sale.getSeller().fullName() : null),
                            str(sale.getCashier() != null ? sale.getCashier().fullName() : null),
                            str(sale.getTotal()),
                            str(sale.getPaidAmount()),
                            str(agg.count()),
                            str(agg.totalRefunded()));
                })
                .toList();

        return TabularFileHelper.write(ExportFormat.CSV, headers, rows);
    }

    @Transactional(readOnly = true)
    public SaleDetailResponse getSaleDetail(Long saleId) {
        User user = currentUserService.requireCurrentUser();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvee: " + saleId));
        assertCanViewSale(sale, auth, user);

        List<SaleRefund> refunds = refundRepository.findBySaleIdOrderByCreatedAtDesc(saleId);
        Map<Long, Integer> lineCounts = loadRefundLineCounts(
                refunds.stream().map(SaleRefund::getId).toList());
        BigDecimal totalRefunded = nullToZero(refundRepository.sumRefundedAmountBySale(
                saleId, SaleRefundStatus.COMPLETED));
        List<SaleEvent> events = saleEventRepository.findBySaleIdOrderByOccurredAtAscIdAsc(saleId);

        return SaleDetailResponse.builder()
                .sale(mapper.toSaleResponse(sale))
                .totalRefunded(totalRefunded)
                .refunds(refunds.stream()
                        .map(r -> toRefundSummary(r, lineCounts.getOrDefault(r.getId(), 0)))
                        .toList())
                .timeline(events.stream().map(this::toEventResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public BrowsePageResponse<SaleRefundSummaryResponse> listReturns(SaleRefundBrowseFilterRequest request) {
        ReturnBrowseFilters filters = resolveReturnFilters(request);
        List<SaleRefund> refunds = refundRepository.searchBrowseReturns(
                filters.filterStatus(),
                filters.status(),
                filters.filterSaleId(),
                filters.saleId(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId(),
                PageRequest.of(filters.page(), filters.size()));

        long total = refundRepository.countBrowseReturns(
                filters.filterStatus(),
                filters.status(),
                filters.filterSaleId(),
                filters.saleId(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId());

        Map<Long, Integer> lineCounts = loadRefundLineCounts(
                refunds.stream().map(SaleRefund::getId).toList());

        List<SaleRefundSummaryResponse> items = refunds.stream()
                .map(refund -> toRefundSummary(refund, lineCounts.getOrDefault(refund.getId(), 0)))
                .toList();

        return toPage(items, total, filters.page(), filters.size());
    }

    @Transactional(readOnly = true)
    public byte[] exportReturnsCsv(SaleRefundBrowseFilterRequest request) {
        ReturnBrowseFilters filters = resolveReturnFilters(request);
        List<SaleRefund> refunds = refundRepository.searchBrowseReturns(
                filters.filterStatus(),
                filters.status(),
                filters.filterSaleId(),
                filters.saleId(),
                filters.filterQ(),
                filters.q(),
                filters.filterDateFrom(),
                filters.dateFrom(),
                filters.filterDateTo(),
                filters.dateTo(),
                filters.filterUserId(),
                filters.userId(),
                PageRequest.of(0, EXPORT_LIMIT));

        Map<Long, Integer> lineCounts = loadRefundLineCounts(
                refunds.stream().map(SaleRefund::getId).toList());

        List<String> headers = List.of(
                "id", "refundNumber", "saleId", "saleNumber", "status", "reason",
                "createdBy", "createdAt", "validatedAt", "totalAmount", "lineCount");

        List<List<String>> rows = refunds.stream()
                .map(refund -> {
                    Sale sale = refund.getSale();
                    return List.of(
                            str(refund.getId()),
                            str(refund.getRefundNumber()),
                            str(sale.getId()),
                            str(sale.getSaleNumber()),
                            str(refund.getStatus()),
                            str(refund.getReason()),
                            str(refund.getCashier() != null ? refund.getCashier().fullName() : null),
                            str(refund.getCreatedAt()),
                            str(refund.getValidatedAt()),
                            str(refund.getTotalAmount()),
                            str(lineCounts.getOrDefault(refund.getId(), 0)));
                })
                .toList();

        return TabularFileHelper.write(ExportFormat.CSV, headers, rows);
    }

    @Transactional(readOnly = true)
    public SaleRefundResponse getReturnDetail(Long returnId) {
        User user = currentUserService.requireCurrentUser();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SaleRefund refund = refundRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Retour non trouve: " + returnId));
        assertCanViewReturn(refund, auth, user);
        return refundService.getReturn(returnId);
    }

    private SaleBrowseFilters resolveSaleFilters(SaleBrowseFilterRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long restrictUserId = resolveRestrictUserId(auth);
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 0;
        int size = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), MAX_PAGE_SIZE) : 25;
        String q = normalizeQuery(request.getQ());
        boolean filterQ = q != null;
        return new SaleBrowseFilters(
                request.getStatus() != null,
                request.getStatus(),
                filterQ,
                filterQ ? q : "",
                request.getDateFrom() != null,
                request.getDateFrom(),
                request.getDateTo() != null,
                request.getDateTo(),
                restrictUserId != null,
                restrictUserId,
                page,
                size);
    }

    private ReturnBrowseFilters resolveReturnFilters(SaleRefundBrowseFilterRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long restrictUserId = resolveRestrictUserId(auth);
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 0;
        int size = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), MAX_PAGE_SIZE) : 25;
        String q = normalizeQuery(request.getQ());
        boolean filterQ = q != null;
        return new ReturnBrowseFilters(
                request.getStatus() != null,
                request.getStatus(),
                request.getSaleId() != null,
                request.getSaleId(),
                filterQ,
                filterQ ? q : "",
                request.getDateFrom() != null,
                request.getDateFrom(),
                request.getDateTo() != null,
                request.getDateTo(),
                restrictUserId != null,
                restrictUserId,
                page,
                size);
    }

    private <T> BrowsePageResponse<T> toPage(List<T> items, long total, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return BrowsePageResponse.<T>builder()
                .items(items)
                .totalElements(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    private Map<Long, RefundAggregate> loadRefundAggregates(List<Long> saleIds) {
        if (saleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RefundAggregate> map = new HashMap<>();
        for (Object[] row : refundRepository.aggregateBySaleIds(saleIds, SaleRefundStatus.COMPLETED)) {
            Long saleId = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            BigDecimal total = row[2] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            map.put(saleId, new RefundAggregate(count, total));
        }
        return map;
    }

    private SaleSummaryResponse toSummary(Sale sale, RefundAggregate aggregate) {
        RefundAggregate agg = aggregate != null ? aggregate : RefundAggregate.EMPTY;
        return SaleSummaryResponse.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .status(sale.getStatus())
                .createdAt(sale.getCreatedAt())
                .paidAt(sale.getPaidAt())
                .validatedAt(sale.getValidatedAt())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().fullName() : null)
                .sellerName(sale.getSeller() != null ? sale.getSeller().fullName() : null)
                .cashierName(sale.getCashier() != null ? sale.getCashier().fullName() : null)
                .total(sale.getTotal())
                .paidAmount(sale.getPaidAmount())
                .refundCount(agg.count())
                .totalRefunded(agg.totalRefunded())
                .build();
    }

    private Map<Long, Integer> loadRefundLineCounts(List<Long> refundIds) {
        if (refundIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : refundRepository.countLinesByRefundIds(refundIds)) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private SaleRefundSummaryResponse toRefundSummary(SaleRefund refund, int lineCount) {
        Sale sale = refund.getSale();
        String createdBy = refund.getCashier() != null ? refund.getCashier().fullName() : null;
        return SaleRefundSummaryResponse.builder()
                .id(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .saleId(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .status(refund.getStatus())
                .totalAmount(refund.getTotalAmount())
                .reason(refund.getReason())
                .createdBy(createdBy)
                .createdAt(refund.getCreatedAt())
                .validatedAt(refund.getValidatedAt())
                .lineCount(lineCount)
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

    private Long resolveRestrictUserId(Authentication auth) {
        if (permissionChecker.has(auth, "analytics.sales.read")
                || permissionChecker.has(auth, "pos.report.read")
                || permissionChecker.has(auth, "pos.sale.read")) {
            return null;
        }
        if (permissionChecker.has(auth, "pos.sale.read_own")) {
            return currentUserService.requireCurrentUser().getId();
        }
        return currentUserService.requireCurrentUser().getId();
    }

    private void assertCanViewSale(Sale sale, Authentication auth, User user) {
        Long restrictUserId = resolveRestrictUserId(auth);
        if (restrictUserId == null) {
            return;
        }
        boolean involved = sale.getSeller() != null && Objects.equals(sale.getSeller().getId(), restrictUserId)
                || sale.getCashier() != null && Objects.equals(sale.getCashier().getId(), restrictUserId);
        if (!involved) {
            throw new AccessDeniedException("Acces refuse a cette vente");
        }
    }

    private void assertCanViewReturn(SaleRefund refund, Authentication auth, User user) {
        assertCanViewSale(refund.getSale(), auth, user);
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String str(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private record RefundAggregate(int count, BigDecimal totalRefunded) {
        private static final RefundAggregate EMPTY = new RefundAggregate(0, BigDecimal.ZERO);
    }

    private record SaleBrowseFilters(
            boolean filterStatus, SaleStatus status,
            boolean filterQ, String q,
            boolean filterDateFrom, Instant dateFrom,
            boolean filterDateTo, Instant dateTo,
            boolean filterUserId, Long userId,
            int page, int size) {}

    private record ReturnBrowseFilters(
            boolean filterStatus, SaleRefundStatus status,
            boolean filterSaleId, Long saleId,
            boolean filterQ, String q,
            boolean filterDateFrom, Instant dateFrom,
            boolean filterDateTo, Instant dateTo,
            boolean filterUserId, Long userId,
            int page, int size) {}
}
