package com.erp.products.repository;

import com.erp.products.domain.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByNomContainingIgnoreCase(String nom);
}
