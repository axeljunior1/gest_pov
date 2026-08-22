package com.erp.products.repository;

import com.erp.products.domain.entity.Customer;
import com.erp.products.domain.enums.SaleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNumber(String customerNumber);

    long countByCustomerNumberStartingWith(String prefix);

    List<Customer> findByIsActiveTrueOrderByLastNameAscFirstNameAsc(Pageable pageable);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.isActive = true
            AND (
                LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY c.lastName, c.firstName
            """)
    List<Customer> searchActive(@Param("q") String query);

    /** Clients actifs sans vente comptabilisee depuis :cutoff (jamais achete inclus). */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.isActive = true
            AND NOT EXISTS (
                SELECT 1 FROM Sale s
                WHERE s.customer = c
                AND s.status IN :statuses
                AND COALESCE(s.paidAt, s.validatedAt) >= :cutoff
            )
            ORDER BY c.lastName, c.firstName
            """)
    List<Customer> findInactiveSince(@Param("cutoff") Instant cutoff, @Param("statuses") Collection<SaleStatus> statuses);
}
