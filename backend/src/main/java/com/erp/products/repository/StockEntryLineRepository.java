package com.erp.products.repository;

import com.erp.products.domain.entity.StockEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockEntryLineRepository extends JpaRepository<StockEntryLine, Long> {

    @Query("SELECT COUNT(l) > 0 FROM StockEntryLine l WHERE l.packaging.id = :packagingId")
    boolean existsByPackagingId(@Param("packagingId") Long packagingId);
}
