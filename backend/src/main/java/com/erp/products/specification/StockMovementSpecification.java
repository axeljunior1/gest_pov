package com.erp.products.specification;

import com.erp.products.domain.entity.StockMovement;
import com.erp.products.dto.StockMovementSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class StockMovementSpecification {

    private StockMovementSpecification() {}

    public static Specification<StockMovement> fromCriteria(StockMovementSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getProductId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), criteria.getProductId()));
            }
            if (criteria.getWarehouseId() != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), criteria.getWarehouseId()));
            }
            if (criteria.getLocationId() != null) {
                predicates.add(cb.equal(root.get("location").get("id"), criteria.getLocationId()));
            }
            if (criteria.getMovementType() != null) {
                predicates.add(cb.equal(root.get("movementType"), criteria.getMovementType()));
            }
            if (criteria.getReferenceType() != null && !criteria.getReferenceType().isBlank()) {
                predicates.add(cb.equal(root.get("referenceType"), criteria.getReferenceType()));
            }
            if (criteria.getReferenceId() != null) {
                predicates.add(cb.equal(root.get("referenceId"), criteria.getReferenceId()));
            }
            if (criteria.getReference() != null && !criteria.getReference().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("reference")),
                        "%" + criteria.getReference().toLowerCase() + "%"));
            }
            if (criteria.getCreatedBy() != null && !criteria.getCreatedBy().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("utilisateur")),
                        "%" + criteria.getCreatedBy().toLowerCase() + "%"));
            }
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("movementDate"), criteria.getDateFrom()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("movementDate"), criteria.getDateTo()));
            }

            query.orderBy(cb.desc(root.get("movementDate")), cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
