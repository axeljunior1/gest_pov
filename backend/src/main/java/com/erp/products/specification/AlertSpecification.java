package com.erp.products.specification;

import com.erp.products.domain.entity.Alert;
import com.erp.products.domain.enums.AlertStatus;
import com.erp.products.domain.enums.AlertType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AlertSpecification {

    private AlertSpecification() {}

    public static Specification<Alert> withFilters(
            AlertType type,
            AlertStatus status,
            Long productId,
            Long warehouseId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }

            query.orderBy(cb.desc(root.get("lastTriggeredAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
