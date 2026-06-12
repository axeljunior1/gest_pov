package com.erp.products.repository;

import com.erp.products.domain.entity.StockEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryLineRepository extends JpaRepository<StockEntryLine, Long> {
}
