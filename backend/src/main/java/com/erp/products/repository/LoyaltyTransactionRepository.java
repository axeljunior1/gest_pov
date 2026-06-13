package com.erp.products.repository;

import com.erp.products.domain.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<LoyaltyTransaction> findBySaleIdOrderByCreatedAtAsc(Long saleId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = com.erp.products.domain.enums.LoyaltyTransactionType.EARN
                THEN t.points ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = com.erp.products.domain.enums.LoyaltyTransactionType.REDEEM
                THEN ABS(t.points) ELSE 0 END), 0)
            FROM LoyaltyTransaction t WHERE t.customer.id = :customerId
            """)
    Object[] sumEarnedAndRedeemed(@Param("customerId") Long customerId);
}
