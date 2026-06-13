package com.erp.products.repository;

import com.erp.products.domain.entity.ProductPackaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductPackagingRepository extends JpaRepository<ProductPackaging, Long> {

    List<ProductPackaging> findByProductIdOrderByNomAsc(Long productId);

    List<ProductPackaging> findByProductIdAndActifTrueOrderByNomAsc(Long productId);

    Optional<ProductPackaging> findByIdAndProductId(Long id, Long productId);

    Optional<ProductPackaging> findFirstByProductIdAndNomIgnoreCase(Long productId, String nom);

    Optional<ProductPackaging> findFirstByProductIdAndDefaultVenteTrueAndActifTrue(Long productId);

    @Query("""
            SELECT p FROM ProductPackaging p
            WHERE LOWER(TRIM(p.codeBarre)) = LOWER(TRIM(:barcode))
            AND p.actif = true
            """)
    Optional<ProductPackaging> findActiveByCodeBarre(@Param("barcode") String barcode);
}
