package com.erp.products.repository;

import com.erp.products.domain.entity.PosSession;
import com.erp.products.domain.enums.PosSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PosSessionRepository extends JpaRepository<PosSession, Long> {

    long countBySessionNumberStartingWith(String prefix);

    Optional<PosSession> findByCashierIdAndStatus(Long cashierId, PosSessionStatus status);
}
