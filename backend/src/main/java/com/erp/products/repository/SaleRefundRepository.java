package com.erp.products.repository;

import com.erp.products.domain.entity.SaleRefund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRefundRepository extends JpaRepository<SaleRefund, Long> {

    long countByRefundNumberStartingWith(String prefix);
}
