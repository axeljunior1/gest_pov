package com.erp.products.repository;

import com.erp.products.domain.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findAllByOrderByNomAsc();

    List<Brand> findByNomContainingIgnoreCaseOrderByNomAsc(String nom);

    Optional<Brand> findFirstByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCase(String nom);
}
