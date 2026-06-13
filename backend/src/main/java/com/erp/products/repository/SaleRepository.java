package com.erp.products.repository;

import com.erp.products.domain.entity.Sale;
import com.erp.products.domain.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    long countBySaleNumberStartingWith(String prefix);

    Optional<Sale> findBySaleNumber(String saleNumber);

    List<Sale> findByPosSessionIdAndStatusOrderByCreatedAtDesc(Long sessionId, SaleStatus status);

    List<Sale> findByPosSessionIdOrderByCreatedAtDesc(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT sl.product.id, SUM(sl.quantityInBaseUnit)
            FROM SaleLine sl
            JOIN sl.sale s
            WHERE s.status = com.erp.products.domain.enums.SaleStatus.VALIDATED
            GROUP BY sl.product.id
            ORDER BY SUM(sl.quantityInBaseUnit) DESC
            """)
    List<Object[]> findTopSoldProductIds(org.springframework.data.domain.Pageable pageable);
}
