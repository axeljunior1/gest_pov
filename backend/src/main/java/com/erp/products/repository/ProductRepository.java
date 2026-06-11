package com.erp.products.repository;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p JOIN p.variantes v WHERE v.codeBarre = :codeBarre")
    Optional<Product> findByVariantBarcode(String codeBarre);

    long countByCategorieId(Long categorieId);

    long countByFournisseurPrincipalId(Long fournisseurPrincipalId);

    long countByUnitId(Long unitId);
}
