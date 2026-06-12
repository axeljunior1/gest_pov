package com.erp.products.repository;

import com.erp.products.domain.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LotRepository extends JpaRepository<Lot, Long> {

    Optional<Lot> findByProductIdAndVariantIdAndNumeroLot(Long productId, Long variantId, String numeroLot);

    Optional<Lot> findByProductIdAndVariantIsNullAndNumeroLot(Long productId, String numeroLot);
}
