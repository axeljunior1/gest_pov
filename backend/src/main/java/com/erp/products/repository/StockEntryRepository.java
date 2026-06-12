package com.erp.products.repository;

import com.erp.products.domain.entity.StockEntry;
import com.erp.products.domain.enums.StockEntryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long>, JpaSpecificationExecutor<StockEntry> {

    Optional<StockEntry> findByEntryNumber(String entryNumber);

    long countByEntryNumberStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM StockEntry e WHERE e.id = :id")
    Optional<StockEntry> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(StockEntryStatus status);

    List<StockEntry> findTop10ByStatusOrderByValidatedAtDescCreatedAtDesc(StockEntryStatus status);
}
