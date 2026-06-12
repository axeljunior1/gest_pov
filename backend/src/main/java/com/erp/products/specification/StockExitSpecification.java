package com.erp.products.specification;

import com.erp.products.domain.entity.StockExit;
import com.erp.products.domain.enums.StockExitReason;
import com.erp.products.domain.enums.StockExitStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class StockExitSpecification {

    private StockExitSpecification() {}

    public static Specification<StockExit> withFilters(
            Long productId,
            Long warehouseId,
            StockExitStatus status,
            StockExitReason reason,
            LocalDate dateFrom,
            LocalDate dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                var join = root.join("lignes");
                predicates.add(cb.equal(join.get("product").get("id"), productId));
                query.distinct(true);
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("exitDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("exitDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
