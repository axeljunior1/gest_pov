package com.erp.products.repository;

import com.erp.products.domain.entity.StockExit;
import com.erp.products.domain.enums.StockExitStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockExitRepository extends JpaRepository<StockExit, Long>, JpaSpecificationExecutor<StockExit> {

    Optional<StockExit> findByExitNumber(String exitNumber);

    long countByExitNumberStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM StockExit e WHERE e.id = :id")
    Optional<StockExit> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(StockExitStatus status);

    List<StockExit> findTop10ByStatusOrderByValidatedAtDescCreatedAtDesc(StockExitStatus status);
}
