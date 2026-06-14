package com.erp.products.repository;

import com.erp.products.domain.entity.SaleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleEventRepository extends JpaRepository<SaleEvent, Long> {

    List<SaleEvent> findBySaleIdOrderByOccurredAtAscIdAsc(Long saleId);
}
