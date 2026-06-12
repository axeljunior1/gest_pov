package com.erp.products.repository;

import com.erp.products.domain.entity.StockTransfer;
import com.erp.products.domain.enums.StockTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findByReference(String reference);

    List<StockTransfer> findByStatusOrderByCreatedAtDesc(StockTransferStatus status);
}
