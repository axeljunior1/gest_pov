package com.erp.products.repository;

import com.erp.products.domain.entity.SaleExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleExchangeRepository extends JpaRepository<SaleExchange, Long> {

    Optional<SaleExchange> findTopByExchangeNumberStartingWithOrderByExchangeNumberDesc(String prefix);
}
