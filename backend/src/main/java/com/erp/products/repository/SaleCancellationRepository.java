package com.erp.products.repository;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.enums.SaleCancellationReason;
import com.erp.products.domain.enums.SaleStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SaleCancellationRepository {

    private final EntityManager em;

    public List<Sale> findCancelled(
            Instant from,
            Instant to,
            Long sellerId,
            Long cashierId,
            Long customerId,
            SaleCancellationReason reason,
            BigDecimal amountMin,
            BigDecimal amountMax,
            int page,
            int size) {

        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT s FROM Sale s
                LEFT JOIN FETCH s.seller
                LEFT JOIN FETCH s.cashier
                LEFT JOIN FETCH s.customer
                LEFT JOIN FETCH s.cancelledBy
                LEFT JOIN FETCH s.createdBy
                LEFT JOIN FETCH s.updatedBy
                WHERE s.status = :cancelled
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);

        if (from != null) {
            jpql.append(" AND s.cancelledAt >= :from");
            params.put("from", from);
        }
        if (to != null) {
            jpql.append(" AND s.cancelledAt < :to");
            params.put("to", to);
        }
        if (sellerId != null) {
            jpql.append(" AND s.seller.id = :sellerId");
            params.put("sellerId", sellerId);
        }
        if (cashierId != null) {
            jpql.append(" AND s.cashier.id = :cashierId");
            params.put("cashierId", cashierId);
        }
        if (customerId != null) {
            jpql.append(" AND s.customer.id = :customerId");
            params.put("customerId", customerId);
        }
        if (reason != null) {
            jpql.append(" AND s.cancellationReason = :reason");
            params.put("reason", reason);
        }
        if (amountMin != null) {
            jpql.append(" AND s.total >= :amountMin");
            params.put("amountMin", amountMin);
        }
        if (amountMax != null) {
            jpql.append(" AND s.total <= :amountMax");
            params.put("amountMax", amountMax);
        }

        jpql.append(" ORDER BY s.cancelledAt DESC, s.id DESC");

        TypedQuery<Sale> query = em.createQuery(jpql.toString(), Sale.class);
        params.forEach(query::setParameter);
        query.setFirstResult(Math.max(page, 0) * size);
        query.setMaxResults(Math.min(Math.max(size, 1), 200));
        return query.getResultList();
    }

    public long countCancelled(
            Instant from,
            Instant to,
            Long sellerId,
            Long cashierId,
            Long customerId,
            SaleCancellationReason reason,
            BigDecimal amountMin,
            BigDecimal amountMax) {

        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(s) FROM Sale s
                WHERE s.status = :cancelled
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("cancelled", SaleStatus.CANCELLED);

        if (from != null) {
            jpql.append(" AND s.cancelledAt >= :from");
            params.put("from", from);
        }
        if (to != null) {
            jpql.append(" AND s.cancelledAt < :to");
            params.put("to", to);
        }
        if (sellerId != null) {
            jpql.append(" AND s.seller.id = :sellerId");
            params.put("sellerId", sellerId);
        }
        if (cashierId != null) {
            jpql.append(" AND s.cashier.id = :cashierId");
            params.put("cashierId", cashierId);
        }
        if (customerId != null) {
            jpql.append(" AND s.customer.id = :customerId");
            params.put("customerId", customerId);
        }
        if (reason != null) {
            jpql.append(" AND s.cancellationReason = :reason");
            params.put("reason", reason);
        }
        if (amountMin != null) {
            jpql.append(" AND s.total >= :amountMin");
            params.put("amountMin", amountMin);
        }
        if (amountMax != null) {
            jpql.append(" AND s.total <= :amountMax");
            params.put("amountMax", amountMax);
        }

        var query = em.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }
}
