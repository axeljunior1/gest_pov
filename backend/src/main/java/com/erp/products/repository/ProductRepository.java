package com.erp.products.repository;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p JOIN p.variantes v WHERE v.codeBarre = :codeBarre")
    Optional<Product> findByVariantBarcode(String codeBarre);

    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(TRIM(p.codeBarre)) = LOWER(TRIM(:barcode))
            """)
    Optional<Product> findByCodeBarreNormalized(@Param("barcode") String barcode);

    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(TRIM(p.codeBarre)) = LOWER(TRIM(:barcode))
            AND p.statut = com.erp.products.domain.enums.ProductStatus.ACTIF
            AND p.cycleVie = com.erp.products.domain.enums.LifecycleStatus.ACTIF
            AND p.isSellable = true
            AND p.hasVariants = false
            """)
    Optional<Product> findActiveSimpleByCodeBarre(@Param("barcode") String barcode);

    long countByCategorieId(Long categorieId);

    long countByBrand_Id(Long brandId);

    long countByStatutAndCycleVie(ProductStatus statut, LifecycleStatus cycleVie);

    long countByFournisseurPrincipalId(Long fournisseurPrincipalId);

    long countByUnitId(Long unitId);
}
