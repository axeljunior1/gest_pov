package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.entity.ProductVariant;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.ProductRepository;
import com.erp.products.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.erp.products.security.TestPermissionChecker;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestPermissionChecker.class)
@Transactional
class ProductVariantPolicyServiceTest {

    @Autowired
    private ProductVariantPolicyService policyService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    private Product simpleProduct;
    private Product parentProduct;
    private ProductVariant variantA;
    private ProductVariant variantB;

    @BeforeEach
    void setUp() {
        simpleProduct = productRepository.save(Product.builder()
                .nom("Sac ciment")
                .sku("SAC-" + System.nanoTime())
                .statut(ProductStatus.ACTIF)
                .cycleVie(LifecycleStatus.ACTIF)
                .hasVariants(false)
                .isSellable(true)
                .isStockable(true)
                .build());

        parentProduct = productRepository.save(Product.builder()
                .nom("Seau")
                .sku("SEA-" + System.nanoTime())
                .statut(ProductStatus.ACTIF)
                .cycleVie(LifecycleStatus.ACTIF)
                .hasVariants(true)
                .isSellable(false)
                .isStockable(false)
                .build());

        variantA = variantRepository.save(ProductVariant.builder()
                .product(parentProduct)
                .name("Seau Rouge")
                .sku(parentProduct.getSku() + "-R")
                .prix(java.math.BigDecimal.valueOf(1000))
                .isSellable(true)
                .isStockable(true)
                .isActive(true)
                .stock(0)
                .build());

        variantB = variantRepository.save(ProductVariant.builder()
                .product(parentProduct)
                .name("Seau Bleu")
                .sku(parentProduct.getSku() + "-B")
                .prix(java.math.BigDecimal.valueOf(1100))
                .isSellable(true)
                .isStockable(true)
                .isActive(true)
                .stock(0)
                .build());
    }

    @Test
    void simpleProductIsSellableAndStockableWithoutVariant() {
        assertNull(policyService.resolveForSale(simpleProduct, null));
        assertNull(policyService.resolveForStock(simpleProduct, null));
    }

    @Test
    void parentWithVariantsCannotBeSoldOrStockedDirectly() {
        BusinessException sale = assertThrows(BusinessException.class,
                () -> policyService.resolveForSale(parentProduct, null));
        assertEquals(ProductVariantPolicyService.MSG_SELECT_VARIANT, sale.getMessage());

        BusinessException stock = assertThrows(BusinessException.class,
                () -> policyService.resolveForStock(parentProduct, null));
        assertEquals(ProductVariantPolicyService.MSG_PARENT_NOT_STOCKABLE, stock.getMessage());
    }

    @Test
    void variantIsSellableAndStockable() {
        ProductVariant resolvedSale = policyService.resolveForSale(parentProduct, variantA.getId());
        assertEquals(variantA.getId(), resolvedSale.getId());

        ProductVariant resolvedStock = policyService.resolveForStock(parentProduct, variantB.getId());
        assertEquals(variantB.getId(), resolvedStock.getId());
    }

    @Test
    void syncProductFlagsUpdatesParent() {
        Product fresh = productRepository.save(Product.builder()
                .nom("Parent sync")
                .sku("PSYNC-" + System.nanoTime())
                .build());
        variantRepository.save(ProductVariant.builder()
                .product(fresh)
                .name("V1")
                .sku(fresh.getSku() + "-V1")
                .stock(0)
                .build());

        policyService.syncProductFlags(fresh.getId());
        Product updated = productRepository.findById(fresh.getId()).orElseThrow();
        assertTrue(updated.getHasVariants());
        assertFalse(updated.getIsSellable());
        assertFalse(updated.getIsStockable());
    }

    @Test
    void inactiveLifecycleProductCannotBeSold() {
        simpleProduct.setCycleVie(LifecycleStatus.SUSPENDU);
        productRepository.save(simpleProduct);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> policyService.resolveForSale(simpleProduct, null));
        assertEquals(ProductEligibilityService.MSG_PRODUCT_LIFECYCLE, ex.getMessage());
    }

    @Test
    void inactiveVariantCannotBeSold() {
        variantA.setIsActive(false);
        variantRepository.save(variantA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> policyService.resolveForSale(parentProduct, variantA.getId()));
        assertEquals(ProductVariantPolicyService.MSG_VARIANT_INACTIVE, ex.getMessage());
    }

    @Test
    void singleVariantIsAutoSelectedForSale() {
        variantRepository.delete(variantB);
        parentProduct.setHasVariants(true);
        productRepository.save(parentProduct);

        ProductVariant resolved = policyService.resolveForSale(parentProduct, null);
        assertEquals(variantA.getId(), resolved.getId());
    }

    @Test
    void nonSellableVariantCannotBeSold() {
        variantA.setIsSellable(false);
        variantRepository.save(variantA);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> policyService.resolveForSale(parentProduct, variantA.getId()));
        assertEquals(ProductVariantPolicyService.MSG_VARIANT_NOT_SELLABLE, ex.getMessage());
    }

    @Test
    void nonStockableVariantCannotReceiveStock() {
        variantB.setIsStockable(false);
        variantRepository.save(variantB);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> policyService.resolveForStock(parentProduct, variantB.getId()));
        assertEquals(ProductVariantPolicyService.MSG_VARIANT_NOT_STOCKABLE, ex.getMessage());
    }
}
