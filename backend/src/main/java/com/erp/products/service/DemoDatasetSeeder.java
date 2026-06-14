package com.erp.products.service;

import com.erp.products.config.DemoSecurityRunner;
import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.LifecycleStatus;
import com.erp.products.domain.enums.PaymentMethod;
import com.erp.products.domain.enums.PosSessionType;
import com.erp.products.domain.enums.ProductStatus;
import com.erp.products.domain.enums.SaleCancellationReason;
import com.erp.products.dto.*;
import com.erp.products.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDatasetSeeder {

    public static final String MARKER_SKU = "DEMO-CAFE-250";
    public static final String PRIMARY_SKU = "DEMO-EAU-1L";
    public static final String DEMO_SELLER_EMAIL = "vendeur@erp.local";
    public static final String DEMO_SELLER_PASSWORD = "Vendeur2026!";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final PackagingService packagingService;
    private final StockService stockService;
    private final PosSaleService posSaleService;
    private final PosSessionService posSessionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public DemoSeedResult seed() {
        if (productRepository.existsBySku(MARKER_SKU)) {
            return DemoSeedResult.alreadyExists();
        }

        UnitOfMeasure unitPcs = unit("pcs");
        UnitOfMeasure unitL = unit("L");
        UnitOfMeasure unitKg = unit("kg");

        Warehouse warehouse = warehouseRepository.findByCode("WH-MAIN")
                .orElseGet(() -> {
                    Warehouse wh = warehouseRepository.save(Warehouse.builder()
                            .code("WH-MAIN")
                            .nom("Entrepot principal")
                            .adresse("Site principal")
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

        Category alimentaire = category("Alimentaire demo", null);
        Category epicerie = category("Epicerie demo", alimentaire);
        Category boissons = category("Boissons demo", alimentaire);
        Category textile = category("Textile demo", null);
        Category hygiene = category("Hygiene demo", null);

        Supplier supAgro = supplier("Agro Demo SA", "DEMO-SUP-AGRO");
        Supplier supTextile = supplier("Textile Demo SARL", "DEMO-SUP-TEXTILE");

        int products = 0;
        products += seedSimpleProduct(
                "Cafe moulu 250g", MARKER_SKU, epicerie, supAgro, unitKg,
                "3.90", "2.40", null, "DEMO-CAFE-BAR", 85);
        products += seedEauProduct(boissons, supAgro, unitL, warehouse, location);
        products += seedTshirtProduct(textile, supTextile, unitPcs, warehouse, location);
        products += seedRizProduct(epicerie, supAgro, unitKg, warehouse, location);
        products += seedSimpleProduct(
                "Savon liquide 500ml", "DEMO-SAVON-500", hygiene, supAgro, unitPcs,
                "2.80", "1.20", null, "DEMO-SAVON-BAR", 0);
        products += seedSimpleProduct(
                "The vert bio 100g", "DEMO-THE-VERT", epicerie, supAgro, unitPcs,
                "4.20", "2.10", null, "DEMO-THE-BAR", 8);

        int customers = seedCustomers();
        User seller = ensureDemoSeller();
        User cashier = loadUser("caissier@erp.local", "cashier@erp.local");
        User admin = loadUser("admin@erp.local");
        int sales = seedPosScenarios(warehouse, seller, cashier, admin);

        log.info("Jeu demo V13 — {} produits, {} clients, {} ventes POS", products, customers, sales);
        return DemoSeedResult.created(products, customers, sales);
    }

    private int seedEauProduct(Category category, Supplier supplier, UnitOfMeasure unit,
                               Warehouse warehouse, Location location) {
        if (productRepository.existsBySku(PRIMARY_SKU)) {
            return 0;
        }
        ProductVariantRequest variantReq = variantReq("Standard", "Unite", PRIMARY_SKU + "-U", "500", "DEMO-EAU-UNIT");
        ProductRequest request = baseProductRequest(
                "Eau minerale 1L", PRIMARY_SKU, category, supplier, unit,
                "500", "280", null, null);
        request.setVariantes(List.of(variantReq));
        ProductResponse created = productService.create(request);
        ProductVariant variant = variantRepository.findByProductId(created.getId()).get(0);
        Long productId = created.getId();

        packagingService.create(productId, packagingReq("Unite", 1, "500", "DEMO-EAU-UNIT-PKG", true, variant.getId()));
        packagingService.create(productId, packagingReq("Carton", 12, "5500", "DEMO-EAU-CARTON", false, variant.getId()));
        packagingService.create(productId, packagingReq("Palette", 600, "250000", "DEMO-EAU-PALETTE", false, variant.getId()));
        receiveStock(productId, variant.getId(), warehouse.getId(), location.getId(), "500");
        return 1;
    }

    private int seedTshirtProduct(Category category, Supplier supplier, UnitOfMeasure unit,
                                  Warehouse warehouse, Location location) {
        String sku = "DEMO-TSHIRT-UV";
        if (productRepository.existsBySku(sku)) {
            return 0;
        }
        List<ProductVariantRequest> variants = List.of(
                variantReq("Noir", "M", sku + "-NM", "19.90", "DEMO-TSHIRT-NM"),
                variantReq("Noir", "L", sku + "-NL", "19.90", "DEMO-TSHIRT-NL"),
                variantReq("Blanc", "M", sku + "-BM", "19.90", "DEMO-TSHIRT-BM"),
                variantReq("Blanc", "L", sku + "-BL", "19.90", "DEMO-TSHIRT-BL"));
        ProductRequest request = baseProductRequest(
                "T-shirt coton unisex", sku, category, supplier, unit,
                "19.90", "8.50", null, null);
        request.setVariantes(variants);
        ProductResponse created = productService.create(request);
        List<ProductVariant> variantList = variantRepository.findByProductId(created.getId());
        int[] stocks = {45, 30, 22, 18};
        for (int i = 0; i < variantList.size(); i++) {
            receiveStock(created.getId(), variantList.get(i).getId(),
                    warehouse.getId(), location.getId(), String.valueOf(stocks[i]));
        }
        return 1;
    }

    private int seedRizProduct(Category category, Supplier supplier, UnitOfMeasure unit,
                               Warehouse warehouse, Location location) {
        String sku = "DEMO-RIZ-1KG";
        if (productRepository.existsBySku(sku)) {
            return 0;
        }
        ProductRequest request = baseProductRequest(
                "Riz basmati 1 kg", sku, category, supplier, unit,
                "2.60", "1.40", null, null);
        ProductResponse created = productService.create(request);
        Long productId = created.getId();
        packagingService.create(productId, packagingReq("Sachet 1 kg", 1, "2.60", "DEMO-RIZ-SACHET", true, null));
        packagingService.create(productId, packagingReq("Sac 5 kg", 5, "11.50", "DEMO-RIZ-SAC5", false, null));
        receiveStock(productId, null, warehouse.getId(), location.getId(), "200");
        return 1;
    }

    private int seedSimpleProduct(String nom, String sku, Category category, Supplier supplier,
                                  UnitOfMeasure unit, String prixVente, String prixAchat,
                                  String promo, String barcode, int stockQty) {
        if (productRepository.existsBySku(sku)) {
            return 0;
        }
        ProductRequest request = baseProductRequest(nom, sku, category, supplier, unit, prixVente, prixAchat, promo, barcode);
        ProductResponse created = productService.create(request);
        if (stockQty > 0) {
            Warehouse warehouse = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
            Location location = locationRepository.findByWarehouseIdAndCode(warehouse.getId(), "DEFAULT").orElseThrow();
            receiveStock(created.getId(), null, warehouse.getId(), location.getId(), String.valueOf(stockQty));
        }
        return 1;
    }

    private int seedCustomers() {
        if (customerRepository.findByCustomerNumber("CUST-DEMO-001").isPresent()) {
            return 5;
        }
        List<Customer> customers = List.of(
                customer("CUST-DEMO-001", "Awa", "Diallo", "+33601010101", "awa.diallo@demo.local", 120),
                customer("CUST-DEMO-002", "Marc", "Bernard", "+33602020202", "marc.bernard@demo.local", 45),
                customer("CUST-DEMO-003", "Fatou", "Sow", "+33603030303", "fatou.sow@demo.local", 320),
                customer("CUST-DEMO-004", "Lucas", "Martin", "+33604040404", null, 0),
                customer("CUST-DEMO-005", "Entreprise", "Demo Shop", "+33605050505", "contact@demoshop.local", 780));
        customerRepository.saveAll(customers);
        return customers.size();
    }

    private int seedPosScenarios(Warehouse warehouse, User seller, User cashier, User admin) {
        if (saleRepository.count() > 0) {
            return 0;
        }
        Long whId = warehouse.getId();
        Product cafe = productRepository.findBySku(MARKER_SKU).orElseThrow();
        Product eau = productRepository.findBySku(PRIMARY_SKU).orElseThrow();
        Product tshirt = productRepository.findBySku("DEMO-TSHIRT-UV").orElseThrow();
        Long eauVariantId = variantRepository.findByProductId(eau.getId()).get(0).getId();
        Long tshirtVariantId = variantRepository.findByProductId(tshirt.getId()).get(0).getId();

        int created = 0;
        created += paidSale(seller, cashier, whId, cafe.getId(), null, 2, 3);
        created += paidSale(seller, cashier, whId, eau.getId(), eauVariantId, 6, 5);
        created += paidSale(seller, cashier, whId, tshirt.getId(), tshirtVariantId, 1, 2);
        created += pendingPaymentSale(seller, whId, cafe.getId(), null, 4);
        created += holdSale(seller, whId, eau.getId(), eauVariantId, 3);
        created += cancelledSale(admin, whId, cafe.getId(), null, 5);
        return created;
    }

    private int paidSale(User seller, User cashier, Long warehouseId,
                           Long productId, Long variantId, int qty, int daysAgo) {
        BigDecimal openingCash = new BigDecimal("200");
        PosSessionOpenRequest sessionReq = new PosSessionOpenRequest();
        sessionReq.setWarehouseId(warehouseId);
        sessionReq.setSessionType(PosSessionType.SALES);
        sessionReq.setOpeningCashAmount(BigDecimal.ZERO);

        PosSessionOpenRequest cashierReq = new PosSessionOpenRequest();
        cashierReq.setWarehouseId(warehouseId);
        cashierReq.setSessionType(PosSessionType.CASHIER);
        cashierReq.setOpeningCashAmount(openingCash);

        SaleResponse sale = DemoSecurityRunner.runAs(seller, () -> {
            posSessionService.openSession(sessionReq);
            SaleResponse draft = posSaleService.createSale();
            SaleLineRequest line = lineReq(productId, variantId, qty);
            SaleResponse withLine = posSaleService.upsertLine(draft.getId(), line);
            return posSaleService.submitForPayment(withLine.getId());
        });

        DemoSecurityRunner.runAs(cashier, () -> {
            posSessionService.openSession(cashierReq);
            SaleValidateRequest validate = new SaleValidateRequest();
            SaleValidateRequest.PaymentInput payment = new SaleValidateRequest.PaymentInput();
            payment.setMethod(PaymentMethod.CASH);
            payment.setAmount(sale.getTotal());
            validate.setPayments(List.of(payment));
            validate.setCashReceived(sale.getTotal());
            posSaleService.validateSale(sale.getId(), validate);
            posSessionService.closeSession(closeReq(openingCash.add(sale.getTotal())));
            return null;
        });

        DemoSecurityRunner.runAs(seller, () -> {
            posSessionService.closeSession(closeReq(BigDecimal.ZERO));
            return null;
        });

        backdateSale(sale.getId(), daysAgo);
        return 1;
    }

    private int pendingPaymentSale(User seller, Long warehouseId, Long productId, Long variantId, int qty) {
        PosSessionOpenRequest sessionReq = new PosSessionOpenRequest();
        sessionReq.setWarehouseId(warehouseId);
        sessionReq.setSessionType(PosSessionType.SALES);
        sessionReq.setOpeningCashAmount(BigDecimal.ZERO);

        DemoSecurityRunner.runAs(seller, () -> {
            posSessionService.openSession(sessionReq);
            SaleResponse draft = posSaleService.createSale();
            posSaleService.upsertLine(draft.getId(), lineReq(productId, variantId, qty));
            posSaleService.submitForPayment(draft.getId());
            posSessionService.closeSession(closeReq(BigDecimal.ZERO));
            return null;
        });
        return 1;
    }

    private int holdSale(User seller, Long warehouseId, Long productId, Long variantId, int qty) {
        PosSessionOpenRequest sessionReq = new PosSessionOpenRequest();
        sessionReq.setWarehouseId(warehouseId);
        sessionReq.setSessionType(PosSessionType.SALES);
        sessionReq.setOpeningCashAmount(BigDecimal.ZERO);

        DemoSecurityRunner.runAs(seller, () -> {
            posSessionService.openSession(sessionReq);
            SaleResponse draft = posSaleService.createSale();
            posSaleService.upsertLine(draft.getId(), lineReq(productId, variantId, qty));
            posSaleService.holdSale(draft.getId(), "Client demo — pause");
            posSessionService.closeSession(closeReq(BigDecimal.ZERO));
            return null;
        });
        return 1;
    }

    private int cancelledSale(User admin, Long warehouseId, Long productId, Long variantId, int qty) {
        PosSessionOpenRequest sessionReq = new PosSessionOpenRequest();
        sessionReq.setWarehouseId(warehouseId);
        sessionReq.setSessionType(PosSessionType.SALES);
        sessionReq.setOpeningCashAmount(BigDecimal.ZERO);

        DemoSecurityRunner.runAs(admin, () -> {
            posSessionService.openSession(sessionReq);
            SaleResponse draft = posSaleService.createSale();
            posSaleService.upsertLine(draft.getId(), lineReq(productId, variantId, qty));
            CancelSaleRequest cancel = new CancelSaleRequest();
            cancel.setReason(SaleCancellationReason.CUSTOMER_CHANGED_MIND);
            cancel.setComment("Annulation demo migration V13");
            posSaleService.cancelSale(draft.getId(), cancel);
            posSessionService.closeSession(closeReq(BigDecimal.ZERO));
            return null;
        });
        return 1;
    }

    private void backdateSale(Long saleId, int daysAgo) {
        saleRepository.findById(saleId).ifPresent(sale -> {
            Instant when = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
            sale.setCreatedAt(when);
            sale.setSubmittedAt(when.plus(5, ChronoUnit.MINUTES));
            sale.setValidatedAt(when.plus(12, ChronoUnit.MINUTES));
            sale.setPaidAt(when.plus(12, ChronoUnit.MINUTES));
            saleRepository.save(sale);
        });
    }

    private PosSessionCloseRequest closeReq(BigDecimal closingCash) {
        PosSessionCloseRequest req = new PosSessionCloseRequest();
        req.setClosingCashAmount(closingCash);
        return req;
    }

    private SaleLineRequest lineReq(Long productId, Long variantId, int qty) {
        SaleLineRequest line = new SaleLineRequest();
        line.setProductId(productId);
        line.setVariantId(variantId);
        line.setQuantityInput(new BigDecimal(qty));
        return line;
    }

    private User ensureDemoSeller() {
        Optional<User> existing = userRepository.findByEmailWithRolesAndPermissions(DEMO_SELLER_EMAIL)
                .or(() -> userRepository.findByEmailWithRolesAndPermissions("seller@erp.local"));
        if (existing.isPresent()) {
            return existing.get();
        }
        Role sellerRole = roleRepository.findByCode("SELLER")
                .orElseThrow(() -> new IllegalStateException("Role SELLER absent"));
        User user = userRepository.save(User.builder()
                .firstName("Vendeur")
                .lastName("Demo")
                .email(DEMO_SELLER_EMAIL)
                .passwordHash(passwordEncoder.encode(DEMO_SELLER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(sellerRole)))
                .build());
        return userRepository.findByEmailWithRolesAndPermissions(DEMO_SELLER_EMAIL).orElse(user);
    }

    private User loadUser(String... emails) {
        for (String email : emails) {
            Optional<User> user = userRepository.findByEmailWithRolesAndPermissions(email);
            if (user.isPresent()) {
                return user.get();
            }
        }
        throw new IllegalStateException("Utilisateur demo absent: " + String.join(", ", emails));
    }

    private void receiveStock(Long productId, Long variantId, Long warehouseId, Long locationId, String qty) {
        StockOperationRequest stock = new StockOperationRequest();
        stock.setProductId(productId);
        stock.setVariantId(variantId);
        stock.setWarehouseId(warehouseId);
        stock.setLocationId(locationId);
        stock.setQuantityBase(new BigDecimal(qty));
        stock.setReferenceType("DEMO_SEED");
        stock.setReference("demo-v13");
        stock.setUtilisateur("demo-seed");
        stockService.receive(stock);
    }

    private ProductRequest baseProductRequest(String nom, String sku, Category category, Supplier supplier,
                                              UnitOfMeasure unit, String prixVente, String prixAchat,
                                              String promo, String barcode) {
        ProductRequest request = new ProductRequest();
        request.setNom(nom);
        request.setSku(sku);
        request.setCategorieId(category.getId());
        request.setFournisseurPrincipalId(supplier.getId());
        request.setUnitId(unit.getId());
        request.setPrixVente(new BigDecimal(prixVente));
        request.setPrixAchat(new BigDecimal(prixAchat));
        if (promo != null) {
            request.setPrixPromotionnel(new BigDecimal(promo));
        }
        request.setCodeBarre(barcode);
        request.setStatut(ProductStatus.ACTIF);
        request.setCycleVie(LifecycleStatus.ACTIF);
        request.setUtilisateur("demo-seed");
        return request;
    }

    private ProductVariantRequest variantReq(String couleur, String taille, String sku, String prix, String barcode) {
        ProductVariantRequest v = new ProductVariantRequest();
        v.setCouleur(couleur);
        v.setTaille(taille);
        v.setSku(sku);
        v.setPrix(new BigDecimal(prix));
        v.setCodeBarre(barcode);
        v.setSellable(true);
        v.setStockable(true);
        v.setActive(true);
        v.setStock(0);
        return v;
    }

    private ProductPackagingRequest packagingReq(String nom, int qty, String prix, String barcode,
                                                 boolean defaultVente, Long variantId) {
        ProductPackagingRequest req = new ProductPackagingRequest();
        req.setNom(nom);
        req.setQuantiteBase(new BigDecimal(qty));
        req.setPrixVente(new BigDecimal(prix));
        req.setCodeBarre(barcode);
        req.setDefaultVente(defaultVente);
        req.setUsableForSale(true);
        req.setUsableForPurchase(true);
        req.setActif(true);
        req.setVariantId(variantId);
        return req;
    }

    private Category category(String nom, Category parent) {
        return categoryRepository.findFirstByNomIgnoreCase(nom)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .nom(nom)
                        .parent(parent)
                        .build()));
    }

    private Supplier supplier(String nom, String reference) {
        return supplierRepository.findByNomContainingIgnoreCase(nom).stream()
                .findFirst()
                .orElseGet(() -> supplierRepository.save(Supplier.builder()
                        .nom(nom)
                        .email(reference.toLowerCase(Locale.ROOT).replace('_', '-') + "@demo.local")
                        .build()));
    }

    private UnitOfMeasure unit(String symbole) {
        return unitRepository.findBySymbole(symbole)
                .orElseGet(() -> unitRepository.save(UnitOfMeasure.builder()
                        .nom(switch (symbole) {
                            case "pcs" -> "Piece";
                            case "kg" -> "Kilogramme";
                            case "L" -> "Litre";
                            default -> symbole;
                        })
                        .symbole(symbole)
                        .build()));
    }

    private Customer customer(String number, String first, String last, String phone, String email, int points) {
        return Customer.builder()
                .customerNumber(number)
                .firstName(first)
                .lastName(last)
                .phone(phone)
                .email(email)
                .loyaltyPoints(points)
                .loyaltyTier(points >= 500 ? "GOLD" : points >= 100 ? "SILVER" : "BRONZE")
                .isActive(true)
                .build();
    }

    public record DemoSeedResult(
            String status,
            int products,
            int customers,
            int sales,
            String markerSku) {

        static DemoSeedResult created(int products, int customers, int sales) {
            return new DemoSeedResult("CREATED", products, customers, sales, PRIMARY_SKU);
        }

        static DemoSeedResult alreadyExists() {
            return new DemoSeedResult("ALREADY_EXISTS", 0, 0, 0, PRIMARY_SKU);
        }
    }
}
