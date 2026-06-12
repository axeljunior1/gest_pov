package com.erp.products.specification;

import com.erp.products.domain.entity.InventoryCount;
import com.erp.products.domain.enums.InventoryCountStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InventoryCountSpecification {

    private InventoryCountSpecification() {}

    public static Specification<InventoryCount> withFilters(
            Long warehouseId,
            Long locationId,
            InventoryCountStatus status,
            Instant dateFrom,
            Instant dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
