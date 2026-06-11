package com.erp.products.specification;

import com.erp.products.domain.entity.Product;
import com.erp.products.dto.ProductSearchCriteria;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> fromCriteria(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                String pattern = "%" + criteria.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nom")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern),
                        cb.like(cb.lower(root.get("marque")), pattern)
                ));
            }

            if (criteria.getSku() != null && !criteria.getSku().isBlank()) {
                predicates.add(cb.equal(root.get("sku"), criteria.getSku()));
            }

            if (criteria.getMarque() != null && !criteria.getMarque().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("marque")), "%" + criteria.getMarque().toLowerCase() + "%"));
            }

            if (criteria.getCategorieId() != null) {
                predicates.add(cb.equal(root.get("categorie").get("id"), criteria.getCategorieId()));
            }

            if (criteria.getFournisseurId() != null) {
                Join<Object, Object> suppliers = root.join("fournisseurs", JoinType.LEFT);
                predicates.add(cb.equal(suppliers.get("supplier").get("id"), criteria.getFournisseurId()));
            }

            if (criteria.getStatut() != null) {
                predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
            }

            if (criteria.getCycleVie() != null) {
                predicates.add(cb.equal(root.get("cycleVie"), criteria.getCycleVie()));
            }

            if (criteria.getPrixMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("prixVente"), criteria.getPrixMin()));
            }

            if (criteria.getPrixMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("prixVente"), criteria.getPrixMax()));
            }

            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedFrom()));
            }

            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedTo()));
            }

            if (criteria.getUpdatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedFrom()));
            }

            if (criteria.getUpdatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), criteria.getUpdatedTo()));
            }

            if (Boolean.TRUE.equals(criteria.getStockFaible()) || Boolean.TRUE.equals(criteria.getRupture())) {
                Join<Object, Object> variants = root.join("variantes", JoinType.LEFT);
                query.groupBy(root.get("id"));

                if (Boolean.TRUE.equals(criteria.getRupture())) {
                    predicates.add(cb.or(
                            cb.equal(variants.get("stock"), 0),
                            cb.isNull(variants.get("stock"))
                    ));
                } else if (Boolean.TRUE.equals(criteria.getStockFaible())) {
                    int seuil = criteria.getStockSeuil() != null ? criteria.getStockSeuil() : 10;
                    predicates.add(cb.and(
                            cb.greaterThan(variants.get("stock"), 0),
                            cb.lessThanOrEqualTo(variants.get("stock"), seuil)
                    ));
                }
            }

            if (criteria.getCodeBarre() != null && !criteria.getCodeBarre().isBlank()) {
                Join<Object, Object> variants = root.join("variantes", JoinType.INNER);
                predicates.add(cb.equal(variants.get("codeBarre"), criteria.getCodeBarre()));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
