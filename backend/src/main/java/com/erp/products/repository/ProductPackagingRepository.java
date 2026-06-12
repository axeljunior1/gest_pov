package com.erp.products.repository;

import com.erp.products.domain.entity.ProductPackaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductPackagingRepository extends JpaRepository<ProductPackaging, Long> {

    List<ProductPackaging> findByProductIdOrderByNomAsc(Long productId);

    Optional<ProductPackaging> findByIdAndProductId(Long id, Long productId);

    Optional<ProductPackaging> findFirstByProductIdAndNomIgnoreCase(Long productId, String nom);
}
