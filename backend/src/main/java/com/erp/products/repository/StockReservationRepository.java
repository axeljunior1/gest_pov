package com.erp.products.repository;

import com.erp.products.domain.entity.StockReservation;
import com.erp.products.domain.enums.StockReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByStatusOrderByCreatedAtDesc(StockReservationStatus status);

    List<StockReservation> findByProductIdAndStatus(Long productId, StockReservationStatus status);
}
