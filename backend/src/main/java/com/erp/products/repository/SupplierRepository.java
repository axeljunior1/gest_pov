package com.erp.products.repository;

import com.erp.products.domain.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByNomContainingIgnoreCase(String nom);

    Optional<Supplier> findFirstByNomIgnoreCase(String nom);

    Optional<Supplier> findFirstByEmailIgnoreCase(String email);
}
