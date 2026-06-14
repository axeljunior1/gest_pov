package com.erp.products.repository;

import com.erp.products.domain.entity.PosSession;
import com.erp.products.domain.enums.PosSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PosSessionRepository extends JpaRepository<PosSession, Long> {

    long countBySessionNumberStartingWith(String prefix);

    Optional<PosSession> findByCashierIdAndStatus(Long cashierId, PosSessionStatus status);

    Optional<PosSession> findByCashierIdAndStatusAndSessionType(
            Long cashierId, PosSessionStatus status, com.erp.products.domain.enums.PosSessionType sessionType);

    @Query("""
            SELECT s FROM PosSession s
            WHERE s.status = :status
            AND s.sessionType = :sessionType
            AND (:cashierId IS NULL OR s.cashier.id = :cashierId)
            ORDER BY s.closedAt DESC, s.id DESC
            """)
    List<PosSession> findClosedSessions(
            @Param("status") PosSessionStatus status,
            @Param("sessionType") com.erp.products.domain.enums.PosSessionType sessionType,
            @Param("cashierId") Long cashierId,
            Pageable pageable);
}
