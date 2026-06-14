package com.erp.products.repository;

import com.erp.products.domain.entity.ProductVariantAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantAttributeValueRepository extends JpaRepository<ProductVariantAttributeValue, Long> {

    List<ProductVariantAttributeValue> findByVariantIdOrderByAttributeIdAsc(Long variantId);

    void deleteByVariantId(Long variantId);

    boolean existsByAttributeId(Long attributeId);

    @Query("""
            SELECT DISTINCT pv.id FROM ProductVariant pv
            JOIN pv.attributeValues av
            JOIN av.attributeValue val
            WHERE pv.product.id = :productId
            AND LOWER(val.value) LIKE CONCAT('%', :q, '%')
            """)
    List<Long> findVariantIdsByProductAndValueSearch(
            @Param("productId") Long productId,
            @Param("q") String q);

    @Query("""
            SELECT pv.id FROM ProductVariant pv
            JOIN pv.attributeValues av
            WHERE pv.product.id = :productId
            GROUP BY pv.id
            HAVING COUNT(DISTINCT av.attribute.id) = :attributeCount
            AND COUNT(DISTINCT av.attributeValue.id) = :attributeCount
            AND SUM(CASE WHEN av.attributeValue.id IN :valueIds THEN 1 ELSE 0 END) = :attributeCount
            """)
    List<Long> findVariantIdsByExactValueCombination(
            @Param("productId") Long productId,
            @Param("valueIds") List<Long> valueIds,
            @Param("attributeCount") long attributeCount);
}
