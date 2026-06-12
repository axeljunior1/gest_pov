package com.erp.products.specification;

import com.erp.products.domain.entity.StockEntry;
import com.erp.products.domain.enums.StockEntryStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class StockEntrySpecification {

    private StockEntrySpecification() {}

    public static Specification<StockEntry> withFilters(
            Long productId,
            Long supplierId,
            Long warehouseId,
            StockEntryStatus status,
            LocalDate dateFrom,
            LocalDate dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                var join = root.join("lignes");
                predicates.add(cb.equal(join.get("product").get("id"), productId));
                query.distinct(true);
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
