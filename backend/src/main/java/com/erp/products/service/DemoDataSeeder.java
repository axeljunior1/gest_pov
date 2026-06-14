package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.dto.ProductPackagingRequest;
import com.erp.products.dto.StockOperationRequest;
import com.erp.products.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataSeeder {

    public static final String DEMO_PRODUCT_SKU = "DEMO-EAU-1L";
    public static final String DEMO_CATEGORY = "Boissons demo";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductVariantRepository variantRepository;
    private final PackagingService packagingService;
    private final StockService stockService;
    private final ProductVariantPolicyService variantPolicyService;

    @Transactional
    public DemoSeedResult seed() {
        if (productRepository.findBySku(DEMO_PRODUCT_SKU).isPresent()) {
            Product existing = productRepository.findBySku(DEMO_PRODUCT_SKU).orElseThrow();
            return DemoSeedResult.alreadyExists(existing.getId(), DEMO_PRODUCT_SKU);
        }

        UnitOfMeasure unit = unitRepository.findBySymbole("L")
                .or(() -> unitRepository.findBySymbole("pcs"))
                .orElseGet(() -> unitRepository.save(UnitOfMeasure.builder()
                        .nom("Litre")
                        .symbole("L")
                        .build()));

        Category category = categoryRepository.findFirstByNomIgnoreCase(DEMO_CATEGORY)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .nom(DEMO_CATEGORY)
                        .build()));

        Product product = productRepository.save(Product.builder()
                .nom("Eau minerale 1L")
                .sku(DEMO_PRODUCT_SKU)
                .categorie(category)
                .unit(unit)
                .prixVente(new BigDecimal("500"))
                .statut(ProductStatus.ACTIF)
                .cycleVie(LifecycleStatus.ACTIF)
                .build());

        ProductVariant variant = variantRepository.save(ProductVariant.builder()
                .product(product)
                .name("Unite")
                .sku(DEMO_PRODUCT_SKU + "-U")
                .prix(new BigDecimal("500"))
                .codeBarre("DEMO-EAU-UNIT")
                .stock(0)
                .build());
        variantPolicyService.syncProductFlags(product.getId());

        packagingService.create(product.getId(), packagingReq("Unite", 1, "500", "DEMO-EAU-UNIT", true, variant.getId()));
        packagingService.create(product.getId(), packagingReq("Carton", 12, "5500", "DEMO-EAU-CARTON", false, variant.getId()));
        packagingService.create(product.getId(), packagingReq("Palette", 600, "250000", null, false, variant.getId()));

        Warehouse warehouse = warehouseRepository.findByCode("WH-MAIN")
                .orElseGet(() -> {
                    Warehouse wh = warehouseRepository.save(Warehouse.builder()
                            .code("WH-MAIN")
                            .nom("Entrepot principal")
                            .actif(true)
                            .build());
                    locationRepository.save(Location.builder()
                            .warehouse(wh)
                            .code("DEFAULT")
                            .nom("Zone par defaut")
                            .actif(true)
                            .build());
                    return wh;
                });
        Location location = locationRepository.findByWarehouseIdAndCode(warehouse.getId(), "DEFAULT")
                .orElseThrow(() -> new IllegalStateException("Emplacement DEFAULT absent"));

        StockOperationRequest stock = new StockOperationRequest();
        stock.setProductId(product.getId());
        stock.setVariantId(variant.getId());
        stock.setWarehouseId(warehouse.getId());
        stock.setLocationId(location.getId());
        stock.setQuantityBase(new BigDecimal("500"));
        stock.setReferenceType("DEMO_SEED");
        stock.setReference("seed-demo");
        stock.setUtilisateur("system");
        stockService.receive(stock);

        log.info("Jeu de demo charge — produit {} (stock 500 {})", DEMO_PRODUCT_SKU, unit.getSymbole());
        return DemoSeedResult.created(product.getId(), DEMO_PRODUCT_SKU);
    }

    private static ProductPackagingRequest packagingReq(
            String nom, int qty, String prix, String barcode, boolean defaultVente, Long variantId) {
        ProductPackagingRequest req = new ProductPackagingRequest();
        req.setNom(nom);
        req.setQuantiteBase(new BigDecimal(qty));
        req.setPrixVente(new BigDecimal(prix));
        req.setCodeBarre(barcode);
        req.setDefaultVente(defaultVente);
        req.setActif(true);
        req.setVariantId(variantId);
        return req;
    }

    public record DemoSeedResult(String status, Long productId, String productSku) {
        static DemoSeedResult created(Long id, String sku) {
            return new DemoSeedResult("CREATED", id, sku);
        }

        static DemoSeedResult alreadyExists(Long id, String sku) {
            return new DemoSeedResult("ALREADY_EXISTS", id, sku);
        }
    }
}
