package com.erp.products.repository;

import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.SaleRefundStatus;
import com.erp.products.domain.enums.SaleStatus;
import com.erp.products.service.analytics.AnalyticsConstants;
import com.erp.products.service.analytics.ResolvedAnalyticsFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    private static final Set<SaleStatus> STATUSES = AnalyticsConstants.COUNTED_SALE_STATUSES;

    private final EntityManager em;

    public SalesMetrics aggregateSales(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(DISTINCT s.id),
                       COALESCE(SUM(s.total), 0),
                       COALESCE(SUM(s.discountTotal), 0),
                       COALESCE(SUM(s.loyaltyDiscountAmount), 0),
                       COALESCE(AVG(s.total), 0)
                FROM Sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        Object[] row = singleRow(jpql.toString(), params);
        LineMetrics lines = aggregateLines(from, to, filter);
        return new SalesMetrics(
                AnalyticsConstants.longValue(row[0]),
                AnalyticsConstants.decimalValue(row[1]),
                AnalyticsConstants.decimalValue(row[2]),
                AnalyticsConstants.decimalValue(row[3]),
                AnalyticsConstants.decimalValue(row[4]),
                lines.itemsSold(),
                lines.grossProfit()
        );
    }

    public LineMetrics aggregateLines(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COALESCE(SUM(sl.quantityInput), 0),
                       COALESCE(SUM(sl.lineTotal - sl.quantityInBaseUnit * COALESCE(p.prixAchat, 0)), 0),
                       COALESCE(SUM(sl.discountAmount), 0)
                FROM SaleLine sl
                JOIN sl.sale s
                JOIN sl.product p
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        appendLineFilters(filter, jpql, params);
        Object[] row = singleRow(jpql.toString(), params);
        return new LineMetrics(
                AnalyticsConstants.decimalValue(row[0]),
                AnalyticsConstants.decimalValue(row[1]),
                AnalyticsConstants.decimalValue(row[2])
        );
    }

    public BigDecimal aggregateRefunds(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COALESCE(SUM(r.totalAmount), 0)
                FROM SaleRefund r
                JOIN r.sale s
                WHERE r.status = :refundStatus
                  AND r.completedAt >= :from AND r.completedAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("refundStatus", SaleRefundStatus.COMPLETED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        return AnalyticsConstants.decimalValue(singleRow(jpql.toString(), params)[0]);
    }

    public long countCancelledSales(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(s)
                FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        return AnalyticsConstants.longValue(singleRow(jpql.toString(), params)[0]);
    }

    public BigDecimal sumCancelledAmount(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COALESCE(SUM(s.total), 0)
                FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        return AnalyticsConstants.decimalValue(singleRow(jpql.toString(), params)[0]);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopCancellationReasons(Instant from, Instant to, ResolvedAnalyticsFilter filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT s.cancellationReason, COUNT(s), COALESCE(SUM(s.total), 0)
                FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                  AND s.cancellationReason IS NOT NULL
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        jpql.append(" GROUP BY s.cancellationReason ORDER BY COUNT(s) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopCancellationSellers(Instant from, Instant to, ResolvedAnalyticsFilter filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT s.seller.id, s.seller.firstName, s.seller.lastName, COUNT(s), COALESCE(SUM(s.total), 0)
                FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        jpql.append(" GROUP BY s.seller.id, s.seller.firstName, s.seller.lastName ORDER BY COUNT(s) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopCancellationCashiers(Instant from, Instant to, ResolvedAnalyticsFilter filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT s.cashier.id, s.cashier.firstName, s.cashier.lastName, COUNT(s), COALESCE(SUM(s.total), 0)
                FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, false);
        jpql.append(" GROUP BY s.cashier.id, s.cashier.firstName, s.cashier.lastName ORDER BY COUNT(s) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    public long countCancelledSalesToday(Instant from, Instant to) {
        String jpql = """
                SELECT COUNT(s) FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        return AnalyticsConstants.longValue(singleRow(jpql, params)[0]);
    }

    public BigDecimal sumCancelledAmountByUser(Instant from, Instant to, Long userId) {
        String jpql = """
                SELECT COALESCE(SUM(s.total), 0) FROM Sale s
                WHERE s.status = :cancelled
                  AND s.cancelledAt >= :from AND s.cancelledAt < :to
                  AND (s.cancelledBy.id = :userId OR s.seller.id = :userId OR s.cashier.id = :userId)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);
        params.put("from", from);
        params.put("to", to);
        params.put("userId", userId);
        return AnalyticsConstants.decimalValue(singleRow(jpql, params)[0]);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTimelineSales(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT s.validatedAt, s.total
                FROM Sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopProducts(Instant from, Instant to, ResolvedAnalyticsFilter filter, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("""
                SELECT p.id, p.nom, p.sku, COALESCE(c.nom, ''),
                       COALESCE(SUM(sl.quantityInput), 0),
                       COALESCE(SUM(sl.lineTotal), 0),
                       COALESCE(SUM(sl.lineTotal - sl.quantityInBaseUnit * COALESCE(p.prixAchat, 0)), 0),
                       COALESCE(SUM(sl.discountAmount), 0)
                FROM SaleLine sl
                JOIN sl.sale s
                JOIN sl.product p
                LEFT JOIN p.categorie c
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        appendLineFilters(filter, jpql, params);
        jpql.append(" GROUP BY p.id, p.nom, p.sku, c.nom ORDER BY SUM(sl.lineTotal) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return q.getResultList();
    }

    public long countTopProducts(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(DISTINCT p.id)
                FROM SaleLine sl
                JOIN sl.sale s
                JOIN sl.product p
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        appendLineFilters(filter, jpql, params);
        return AnalyticsConstants.longValue(singleRow(jpql.toString(), params)[0]);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findCategoryStats(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT c.id, c.nom,
                       COALESCE(SUM(sl.lineTotal), 0),
                       COALESCE(SUM(sl.quantityInput), 0),
                       COALESCE(SUM(sl.lineTotal - sl.quantityInBaseUnit * COALESCE(p.prixAchat, 0)), 0)
                FROM SaleLine sl
                JOIN sl.sale s
                JOIN sl.product p
                LEFT JOIN p.categorie c
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        appendLineFilters(filter, jpql, params);
        jpql.append(" GROUP BY c.id, c.nom ORDER BY SUM(sl.lineTotal) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findPaymentStats(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p.id)
                FROM Payment p
                JOIN p.sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                  AND p.status = com.erp.products.domain.enums.PaymentStatus.PAID
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        if (filter.getPaymentMethod() != null) {
            jpql.append(" AND p.method = :paymentMethod");
            params.put("paymentMethod", filter.getPaymentMethod());
        }
        jpql.append(" GROUP BY p.method ORDER BY SUM(p.amount) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    public CashSessionMetrics aggregateCashSessions(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COALESCE(SUM(ps.expectedCashAmount), 0),
                       COALESCE(SUM(ps.closingCashAmount), 0),
                       COALESCE(SUM(ps.differenceAmount), 0)
                FROM PosSession ps
                WHERE ps.status = com.erp.products.domain.enums.PosSessionStatus.CLOSED
                  AND ps.closedAt >= :from AND ps.closedAt < :to
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        if (filter.getWarehouseId() != null) {
            jpql.append(" AND ps.warehouse.id = :warehouseId");
            params.put("warehouseId", filter.getWarehouseId());
        }
        if (filter.getCashierId() != null) {
            jpql.append(" AND ps.cashier.id = :cashierId");
            params.put("cashierId", filter.getCashierId());
        }
        Object[] row = singleRow(jpql.toString(), params);
        return new CashSessionMetrics(
                AnalyticsConstants.decimalValue(row[0]),
                AnalyticsConstants.decimalValue(row[1]),
                AnalyticsConstants.decimalValue(row[2])
        );
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findCashierStats(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT s.cashier.id,
                       CONCAT(s.cashier.firstName, ' ', s.cashier.lastName),
                       COUNT(s.id),
                       COALESCE(SUM(s.total), 0),
                       COALESCE(AVG(s.total), 0),
                       COALESCE(SUM(s.discountTotal), 0)
                FROM Sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        jpql.append(" GROUP BY s.cashier.id, s.cashier.firstName, s.cashier.lastName ORDER BY SUM(s.total) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopCustomers(Instant from, Instant to, ResolvedAnalyticsFilter filter, int limit) {
        StringBuilder jpql = new StringBuilder("""
                SELECT c.id, CONCAT(c.firstName, ' ', c.lastName), c.customerNumber,
                       COALESCE(SUM(s.total), 0), COUNT(s.id), COALESCE(AVG(s.total), 0)
                FROM Sale s
                JOIN s.customer c
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        if (filter.getCustomerId() != null) {
            jpql.append(" AND c.id = :customerId");
            params.put("customerId", filter.getCustomerId());
        }
        jpql.append(" GROUP BY c.id, c.firstName, c.lastName, c.customerNumber ORDER BY SUM(s.total) DESC");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        q.setMaxResults(limit);
        return q.getResultList();
    }

    public long countNewCustomers(Instant from, Instant to) {
        String jpql = """
                SELECT COUNT(c)
                FROM Customer c
                WHERE c.createdAt >= :from AND c.createdAt < :to
                """;
        return AnalyticsConstants.longValue(singleRow(jpql, Map.of("from", from, "to", to))[0]);
    }

    public long countReturningCustomers(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(DISTINCT s.customer.id)
                FROM Sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                  AND s.customer IS NOT NULL
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        return AnalyticsConstants.longValue(singleRow(jpql.toString(), params)[0]);
    }

    public long sumLoyaltyPoints(Instant from, Instant to, String type) {
        String jpql = """
                SELECT COALESCE(SUM(ABS(lt.points)), 0)
                FROM LoyaltyTransaction lt
                WHERE lt.type = :type
                  AND lt.createdAt >= :from AND lt.createdAt < :to
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("from", from);
        params.put("to", to);
        params.put("type", com.erp.products.domain.enums.LoyaltyTransactionType.valueOf(type));
        return AnalyticsConstants.longValue(singleRow(jpql, params)[0]);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findProductSalesQuantities(Instant from, Instant to, ResolvedAnalyticsFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT p.id, COALESCE(SUM(sl.quantityInput), 0)
                FROM SaleLine sl
                JOIN sl.sale s
                JOIN sl.product p
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                """);
        Map<String, Object> params = baseSaleParams(from, to, filter, jpql);
        appendLineFilters(filter, jpql, params);
        jpql.append(" GROUP BY p.id");
        Query q = em.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findNeverSoldProducts() {
        Query q = em.createQuery("""
                SELECT p.id, p.nom, p.sku
                FROM Product p
                WHERE p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
                  AND NOT EXISTS (
                      SELECT 1 FROM SaleLine sl
                      JOIN sl.sale s
                      WHERE sl.product = p AND s.status IN :statuses
                  )
                ORDER BY p.nom
                """);
        q.setParameter("statuses", STATUSES);
        q.setMaxResults(20);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findHighDiscountSales(Instant from, Instant to, BigDecimal minDiscount) {
        Query q = em.createQuery("""
                SELECT s.id, s.saleNumber, s.discountTotal, s.total
                FROM Sale s
                WHERE s.status IN :statuses
                  AND s.validatedAt >= :from AND s.validatedAt < :to
                  AND s.discountTotal >= :min
                ORDER BY s.discountTotal DESC
                """);
        q.setParameter("statuses", STATUSES);
        q.setParameter("from", from);
        q.setParameter("to", to);
        q.setParameter("min", minDiscount);
        q.setMaxResults(10);
        return q.getResultList();
    }

    private Map<String, Object> baseSaleParams(Instant from, Instant to, ResolvedAnalyticsFilter filter, StringBuilder jpql) {
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", STATUSES);
        params.put("from", from);
        params.put("to", to);
        appendSaleFilters(filter, jpql, params, true);
        return params;
    }

    private void appendSaleFilters(ResolvedAnalyticsFilter filter, StringBuilder jpql, Map<String, Object> params, boolean includePayment) {
        if (filter.getWarehouseId() != null) {
            jpql.append(" AND s.warehouse.id = :warehouseId");
            params.put("warehouseId", filter.getWarehouseId());
        }
        if (filter.getCashierId() != null) {
            jpql.append(" AND s.cashier.id = :cashierId");
            params.put("cashierId", filter.getCashierId());
        }
        if (filter.getCustomerId() != null) {
            jpql.append(" AND s.customer.id = :customerId");
            params.put("customerId", filter.getCustomerId());
        }
        if (includePayment && filter.getPaymentMethod() != null) {
            jpql.append("""
                     AND EXISTS (
                        SELECT 1 FROM Payment p
                        WHERE p.sale = s AND p.method = :paymentMethod
                          AND p.status = com.erp.products.domain.enums.PaymentStatus.PAID
                     )
                    """);
            params.put("paymentMethod", filter.getPaymentMethod());
        }
    }

    private void appendLineFilters(ResolvedAnalyticsFilter filter, StringBuilder jpql, Map<String, Object> params) {
        if (filter.getCategoryId() != null) {
            jpql.append(" AND p.categorie.id = :categoryId");
            params.put("categoryId", filter.getCategoryId());
        }
        if (filter.getProductId() != null) {
            jpql.append(" AND p.id = :productId");
            params.put("productId", filter.getProductId());
        }
    }

    private Object[] singleRow(String jpql, Map<String, Object> params) {
        Query q = em.createQuery(jpql);
        params.forEach(q::setParameter);
        Object result = q.getSingleResult();
        if (result instanceof Object[] arr) {
            return arr;
        }
        return new Object[]{result};
    }

    public record SalesMetrics(
            long saleCount,
            BigDecimal revenue,
            BigDecimal discounts,
            BigDecimal loyaltyDiscounts,
            BigDecimal averageBasket,
            BigDecimal itemsSold,
            BigDecimal grossProfit
    ) {
    }

    public record LineMetrics(BigDecimal itemsSold, BigDecimal grossProfit, BigDecimal lineDiscounts) {
    }

    public record CashSessionMetrics(BigDecimal expectedCash, BigDecimal declaredCash, BigDecimal difference) {
    }
}
