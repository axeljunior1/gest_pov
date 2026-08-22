package com.erp.products.service;

import com.erp.products.domain.entity.*;
import com.erp.products.domain.enums.*;
import com.erp.products.dto.*;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.*;
import com.erp.products.security.CurrentUserService;
import com.erp.products.util.TabularFileHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final List<String> PRODUCT_HEADERS = List.of(
            "sku", "nom", "description", "marque", "categorieNom", "unitSymbole",
            "prixAchat", "prixVente", "statut", "cycleVie");
    private static final List<String> PACKAGING_HEADERS = List.of(
            "productSku", "nom", "symbole", "quantiteBase", "codeBarre", "principal");
    private static final List<String> INITIAL_STOCK_HEADERS = List.of(
            "productSku", "variantSku", "warehouseCode", "locationCode", "quantity", "lotNumber", "expiryDate");
    private static final List<String> BRAND_HEADERS = List.of("nom");
    private static final List<String> CATEGORY_HEADERS = List.of("nom", "parentNom");
    private static final List<String> SUPPLIER_HEADERS = List.of("nom", "email", "telephone", "adresse");
    private static final List<String> UNIT_HEADERS = List.of("nom", "symbole");
    private static final List<String> WAREHOUSE_HEADERS = List.of("code", "nom", "adresse");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final ProductPackagingRepository packagingRepository;
    private final BarcodeService barcodeService;
    private final BarcodeRegistryService barcodeRegistryService;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final LotRepository lotRepository;
    private final StockService stockService;
    private final ImportJobRepository importJobRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public byte[] productTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, PRODUCT_HEADERS);
    }

    public byte[] packagingTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, PACKAGING_HEADERS);
    }

    public byte[] initialStockTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, INITIAL_STOCK_HEADERS);
    }

    public byte[] brandTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, BRAND_HEADERS);
    }

    public byte[] categoryTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, CATEGORY_HEADERS);
    }

    public byte[] supplierTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, SUPPLIER_HEADERS);
    }

    public byte[] unitTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, UNIT_HEADERS);
    }

    public byte[] warehouseTemplate(ExportFormat format) {
        return TabularFileHelper.template(format, WAREHOUSE_HEADERS);
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewProducts(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildProductPreview(readData(file), duplicateMode, false);
    }

    @Transactional
    public ImportValidateResponse validateProducts(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildProductPreview(rows, duplicateMode, true);
        String actor = currentUserService.resolveActor(user);
        int success = 0;
        for (ImportLineResult line : preview.getLines()) {
            if ("OK".equals(line.getStatus()) && line.getAction() != null && !"SKIP".equals(line.getAction())) {
                applyProductLine(rows.get(line.getDataRowIndex()), duplicateMode, actor);
                success++;
            }
        }
        ImportJob job = saveJob(ImportType.PRODUCTS, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewPackagings(MultipartFile file) {
        return buildPackagingPreview(readData(file), false);
    }

    @Transactional
    public ImportValidateResponse validatePackagings(MultipartFile file, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildPackagingPreview(rows, true);
        String actor = currentUserService.resolveActor(user);
        int success = 0;
        for (ImportLineResult line : preview.getLines()) {
            if ("OK".equals(line.getStatus()) && "CREATE".equals(line.getAction())) {
                applyPackagingLine(rows.get(line.getDataRowIndex()));
                success++;
            }
        }
        ImportJob job = saveJob(ImportType.PACKAGINGS, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewInitialStock(MultipartFile file) {
        return buildInitialStockPreview(readData(file), false);
    }

    @Transactional
    public ImportValidateResponse validateInitialStock(MultipartFile file, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildInitialStockPreview(rows, true);
        String actor = currentUserService.resolveActor(user);
        int success = 0;
        for (ImportLineResult line : preview.getLines()) {
            if ("OK".equals(line.getStatus()) && "CREATE".equals(line.getAction())) {
                applyInitialStockLine(rows.get(line.getDataRowIndex()), actor, file.getOriginalFilename());
                success++;
            }
        }
        ImportJob job = saveJob(ImportType.INITIAL_STOCK, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewBrands(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildBrandPreview(readData(file), duplicateMode);
    }

    @Transactional
    public ImportValidateResponse validateBrands(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildBrandPreview(rows, duplicateMode);
        String actor = currentUserService.resolveActor(user);
        int success = applyOkLines(preview, rows, (row, mode) -> applyBrandLine(row, mode), duplicateMode);
        ImportJob job = saveJob(ImportType.BRANDS, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewCategories(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildCategoryPreview(readData(file), duplicateMode);
    }

    @Transactional
    public ImportValidateResponse validateCategories(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildCategoryPreview(rows, duplicateMode);
        String actor = currentUserService.resolveActor(user);
        int success = applyOkLines(preview, rows, (row, mode) -> applyCategoryLine(row, mode), duplicateMode);
        ImportJob job = saveJob(ImportType.CATEGORIES, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewSuppliers(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildSupplierPreview(readData(file), duplicateMode);
    }

    @Transactional
    public ImportValidateResponse validateSuppliers(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildSupplierPreview(rows, duplicateMode);
        String actor = currentUserService.resolveActor(user);
        int success = applyOkLines(preview, rows, (row, mode) -> applySupplierLine(row, mode), duplicateMode);
        ImportJob job = saveJob(ImportType.SUPPLIERS, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewUnits(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildUnitPreview(readData(file), duplicateMode);
    }

    @Transactional
    public ImportValidateResponse validateUnits(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildUnitPreview(rows, duplicateMode);
        String actor = currentUserService.resolveActor(user);
        int success = applyOkLines(preview, rows, (row, mode) -> applyUnitLine(row, mode), duplicateMode);
        ImportJob job = saveJob(ImportType.UNITS, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewWarehouses(MultipartFile file, DuplicateSkuMode duplicateMode) {
        return buildWarehousePreview(readData(file), duplicateMode);
    }

    @Transactional
    public ImportValidateResponse validateWarehouses(MultipartFile file, DuplicateSkuMode duplicateMode, String user) {
        List<String[]> rows = readData(file);
        ImportPreviewResponse preview = buildWarehousePreview(rows, duplicateMode);
        String actor = currentUserService.resolveActor(user);
        int success = applyOkLines(preview, rows, (row, mode) -> applyWarehouseLine(row, mode), duplicateMode);
        ImportJob job = saveJob(ImportType.WAREHOUSES, file.getOriginalFilename(), actor,
                preview.getTotalRows(), success, preview.getErrorRows(), preview.getLines());
        return ImportValidateResponse.builder().job(toJobResponse(job)).lines(preview.getLines()).build();
    }

    @Transactional(readOnly = true)
    public List<ImportJobResponse> listHistory() {
        return importJobRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getHistory(Long id) {
        return toJobResponse(importJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Import non trouve: " + id)));
    }

    private ImportPreviewResponse buildProductPreview(
            List<String[]> rows, DuplicateSkuMode duplicateMode, boolean forValidate) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), PRODUCT_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String sku = TabularFileHelper.cell(row, 0);
            String nom = TabularFileHelper.cell(row, 1);
            if (sku.isBlank() && nom.isBlank()) {
                continue;
            }
            if (sku.isBlank()) {
                lines.add(error(lineNum, i, sku, "SKU obligatoire"));
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, sku, "Nom obligatoire"));
                continue;
            }
            if (!seenSkus.add(sku.toUpperCase())) {
                lines.add(error(lineNum, i, sku, "SKU duplique dans le fichier"));
                continue;
            }
            Optional<Product> existing = productRepository.findBySku(sku);
            if (existing.isPresent()) {
                lines.add(duplicateResult(lineNum, i, sku, duplicateMode, "SKU deja existant"));
            } else {
                String unitSymbole = TabularFileHelper.cell(row, 5);
                if (unitSymbole.isBlank()) {
                    lines.add(error(lineNum, i, sku, "Unite (unitSymbole) obligatoire pour un nouveau produit"));
                    continue;
                }
                if (unitRepository.findBySymbole(unitSymbole).isEmpty()) {
                    lines.add(error(lineNum, i, sku, "Unite inconnue: " + unitSymbole));
                    continue;
                }
                lines.add(ok(lineNum, i, sku, "CREATE"));
            }
        }
        return summarize(lines);
    }

    private void applyProductLine(String[] row, DuplicateSkuMode duplicateMode, String actor) {
        String sku = TabularFileHelper.cell(row, 0);
        Optional<Product> existing = productRepository.findBySku(sku);
        Category category = resolveCategory(TabularFileHelper.cell(row, 4));
        Brand brand = resolveBrand(TabularFileHelper.cell(row, 3));
        UnitOfMeasure unit = unitRepository.findBySymbole(TabularFileHelper.cell(row, 5))
                .orElseThrow(() -> new BusinessException("Unite inconnue"));
        ProductStatus statut = parseEnum(TabularFileHelper.cell(row, 8), ProductStatus.class, ProductStatus.ACTIF);
        LifecycleStatus cycleVie = parseEnum(TabularFileHelper.cell(row, 9), LifecycleStatus.class, LifecycleStatus.BROUILLON);

        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("SKU existant: " + sku);
            }
            Product product = existing.get();
            product.setNom(TabularFileHelper.cell(row, 1));
            product.setDescription(emptyToNull(TabularFileHelper.cell(row, 2)));
            product.setBrand(brand);
            product.setCategorie(category);
            product.setUnit(unit);
            product.setPrixAchat(parseDecimal(TabularFileHelper.cell(row, 6)));
            product.setPrixVente(parseDecimal(TabularFileHelper.cell(row, 7)));
            product.setStatut(statut);
            product.setCycleVie(cycleVie);
            productRepository.save(product);
        } else {
            Product product = Product.builder()
                    .nom(TabularFileHelper.cell(row, 1))
                    .sku(sku)
                    .description(emptyToNull(TabularFileHelper.cell(row, 2)))
                    .brand(brand)
                    .categorie(category)
                    .unit(unit)
                    .prixAchat(parseDecimal(TabularFileHelper.cell(row, 6)))
                    .prixVente(parseDecimal(TabularFileHelper.cell(row, 7)))
                    .statut(statut)
                    .cycleVie(cycleVie)
                    .build();
            productRepository.save(product);
        }
    }

    private ImportPreviewResponse buildBrandPreview(List<String[]> rows, DuplicateSkuMode duplicateMode) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), BRAND_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String nom = TabularFileHelper.cell(row, 0);
            if (nom.isBlank()) {
                continue;
            }
            String key = nom.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                lines.add(error(lineNum, i, nom, "Marque dupliquee dans le fichier"));
                continue;
            }
            if (brandRepository.findFirstByNomIgnoreCase(nom).isPresent()) {
                lines.add(duplicateResult(lineNum, i, nom, duplicateMode, "Marque deja existante"));
            } else {
                lines.add(ok(lineNum, i, nom, "CREATE"));
            }
        }
        return summarize(lines);
    }

    private void applyBrandLine(String[] row, DuplicateSkuMode duplicateMode) {
        String nom = TabularFileHelper.cell(row, 0).trim();
        Optional<Brand> existing = brandRepository.findFirstByNomIgnoreCase(nom);
        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("Marque existante: " + nom);
            }
            // Unique key is nom — nothing else to update
            brandRepository.save(existing.get());
        } else {
            brandRepository.save(Brand.builder().nom(nom).build());
        }
    }

    private ImportPreviewResponse buildCategoryPreview(List<String[]> rows, DuplicateSkuMode duplicateMode) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), CATEGORY_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> pendingNoms = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String nom = TabularFileHelper.cell(row, 0);
            String parentNom = TabularFileHelper.cell(row, 1);
            if (nom.isBlank() && parentNom.isBlank()) {
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, "", "Nom obligatoire"));
                continue;
            }
            String fileKey = (parentNom.isBlank() ? "" : parentNom.toLowerCase(Locale.ROOT))
                    + "|" + nom.toLowerCase(Locale.ROOT);
            if (!seen.add(fileKey)) {
                lines.add(error(lineNum, i, nom, "Categorie dupliquee dans le fichier"));
                continue;
            }
            Category parent = null;
            if (!parentNom.isBlank()) {
                parent = categoryRepository.findFirstByNomIgnoreCase(parentNom).orElse(null);
                if (parent == null && !pendingNoms.contains(parentNom.toLowerCase(Locale.ROOT))) {
                    lines.add(error(lineNum, i, nom, "Parent inconnu: " + parentNom));
                    continue;
                }
            }
            Optional<Category> existing = (!parentNom.isBlank() && parent == null)
                    ? Optional.empty()
                    : findCategoryByNomAndParent(nom, parent);
            if (existing.isPresent()) {
                lines.add(duplicateResult(lineNum, i, nom, duplicateMode, "Categorie deja existante"));
            } else {
                lines.add(ok(lineNum, i, nom, "CREATE"));
                pendingNoms.add(nom.toLowerCase(Locale.ROOT));
            }
        }
        return summarize(lines);
    }

    private void applyCategoryLine(String[] row, DuplicateSkuMode duplicateMode) {
        String nom = TabularFileHelper.cell(row, 0).trim();
        String parentNom = TabularFileHelper.cell(row, 1);
        Category parent = null;
        if (!parentNom.isBlank()) {
            parent = categoryRepository.findFirstByNomIgnoreCase(parentNom.trim())
                    .orElseThrow(() -> new BusinessException("Parent inconnu: " + parentNom));
        }
        Optional<Category> existing = findCategoryByNomAndParent(nom, parent);
        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("Categorie existante: " + nom);
            }
            categoryRepository.save(existing.get());
        } else {
            categoryRepository.save(Category.builder().nom(nom).parent(parent).build());
        }
    }

    private Optional<Category> findCategoryByNomAndParent(String nom, Category parent) {
        if (parent == null) {
            return categoryRepository.findFirstByNomIgnoreCaseAndParentIsNull(nom);
        }
        return categoryRepository.findFirstByNomIgnoreCaseAndParent_Id(nom, parent.getId());
    }

    private ImportPreviewResponse buildSupplierPreview(List<String[]> rows, DuplicateSkuMode duplicateMode) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), SUPPLIER_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String nom = TabularFileHelper.cell(row, 0);
            String email = TabularFileHelper.cell(row, 1);
            if (nom.isBlank() && email.isBlank()) {
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, email, "Nom obligatoire"));
                continue;
            }
            String key = nom.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                lines.add(error(lineNum, i, nom, "Fournisseur duplique dans le fichier"));
                continue;
            }
            Optional<Supplier> existing = findSupplierByUniqueKey(nom, email);
            if (existing.isPresent()) {
                lines.add(duplicateResult(lineNum, i, nom, duplicateMode, "Fournisseur deja existant"));
            } else {
                lines.add(ok(lineNum, i, nom, "CREATE"));
            }
        }
        return summarize(lines);
    }

    private void applySupplierLine(String[] row, DuplicateSkuMode duplicateMode) {
        String nom = TabularFileHelper.cell(row, 0).trim();
        String email = emptyToNull(TabularFileHelper.cell(row, 1));
        String telephone = emptyToNull(TabularFileHelper.cell(row, 2));
        String adresse = emptyToNull(TabularFileHelper.cell(row, 3));
        Optional<Supplier> existing = findSupplierByUniqueKey(nom, email);
        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("Fournisseur existant: " + nom);
            }
            Supplier supplier = existing.get();
            supplier.setNom(nom);
            supplier.setEmail(email);
            supplier.setTelephone(telephone);
            supplier.setAdresse(adresse);
            supplierRepository.save(supplier);
        } else {
            supplierRepository.save(Supplier.builder()
                    .nom(nom)
                    .email(email)
                    .telephone(telephone)
                    .adresse(adresse)
                    .build());
        }
    }

    private Optional<Supplier> findSupplierByUniqueKey(String nom, String email) {
        Optional<Supplier> byNom = supplierRepository.findFirstByNomIgnoreCase(nom);
        if (byNom.isPresent()) {
            return byNom;
        }
        if (email != null && !email.isBlank()) {
            return supplierRepository.findFirstByEmailIgnoreCase(email.trim());
        }
        return Optional.empty();
    }

    private ImportPreviewResponse buildUnitPreview(List<String[]> rows, DuplicateSkuMode duplicateMode) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), UNIT_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String nom = TabularFileHelper.cell(row, 0);
            String symbole = TabularFileHelper.cell(row, 1);
            if (nom.isBlank() && symbole.isBlank()) {
                continue;
            }
            if (symbole.isBlank()) {
                lines.add(error(lineNum, i, nom, "Symbole obligatoire"));
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, symbole, "Nom obligatoire"));
                continue;
            }
            String key = symbole.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                lines.add(error(lineNum, i, symbole, "Unite dupliquee dans le fichier"));
                continue;
            }
            Optional<UnitOfMeasure> existing = unitRepository.findBySymbole(symbole);
            if (existing.isPresent()) {
                lines.add(duplicateResult(lineNum, i, symbole, duplicateMode, "Unite deja existante"));
            } else {
                Optional<UnitOfMeasure> byNom = unitRepository.findByNomIgnoreCase(nom);
                if (byNom.isPresent()) {
                    lines.add(error(lineNum, i, symbole, "Nom d'unite deja utilise: " + nom));
                    continue;
                }
                lines.add(ok(lineNum, i, symbole, "CREATE"));
            }
        }
        return summarize(lines);
    }

    private void applyUnitLine(String[] row, DuplicateSkuMode duplicateMode) {
        String nom = TabularFileHelper.cell(row, 0).trim();
        String symbole = TabularFileHelper.cell(row, 1).trim();
        Optional<UnitOfMeasure> existing = unitRepository.findBySymbole(symbole);
        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("Unite existante: " + symbole);
            }
            UnitOfMeasure unit = existing.get();
            Optional<UnitOfMeasure> byNom = unitRepository.findByNomIgnoreCase(nom);
            if (byNom.isPresent() && !byNom.get().getId().equals(unit.getId())) {
                throw new BusinessException("Nom d'unite deja utilise: " + nom);
            }
            unit.setNom(nom);
            unitRepository.save(unit);
        } else {
            unitRepository.save(UnitOfMeasure.builder().nom(nom).symbole(symbole).build());
        }
    }

    private ImportPreviewResponse buildWarehousePreview(List<String[]> rows, DuplicateSkuMode duplicateMode) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), WAREHOUSE_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String code = TabularFileHelper.cell(row, 0);
            String nom = TabularFileHelper.cell(row, 1);
            if (code.isBlank() && nom.isBlank()) {
                continue;
            }
            if (code.isBlank()) {
                lines.add(error(lineNum, i, nom, "Code obligatoire"));
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, code, "Nom obligatoire"));
                continue;
            }
            String key = code.toUpperCase(Locale.ROOT);
            if (!seen.add(key)) {
                lines.add(error(lineNum, i, code, "Entrepot duplique dans le fichier"));
                continue;
            }
            Optional<Warehouse> existing = warehouseRepository.findByCode(key);
            if (existing.isPresent()) {
                lines.add(duplicateResult(lineNum, i, code, duplicateMode, "Entrepot deja existant"));
            } else {
                lines.add(ok(lineNum, i, code, "CREATE"));
            }
        }
        return summarize(lines);
    }

    private void applyWarehouseLine(String[] row, DuplicateSkuMode duplicateMode) {
        String code = TabularFileHelper.cell(row, 0).trim().toUpperCase(Locale.ROOT);
        String nom = TabularFileHelper.cell(row, 1).trim();
        String adresse = emptyToNull(TabularFileHelper.cell(row, 2));
        Optional<Warehouse> existing = warehouseRepository.findByCode(code);
        if (existing.isPresent()) {
            if (duplicateMode != DuplicateSkuMode.UPDATE) {
                throw new BusinessException("Entrepot existant: " + code);
            }
            Warehouse warehouse = existing.get();
            warehouse.setNom(nom);
            warehouse.setAdresse(adresse);
            warehouseRepository.save(warehouse);
        } else {
            warehouseRepository.save(Warehouse.builder()
                    .code(code)
                    .nom(nom)
                    .adresse(adresse)
                    .actif(true)
                    .build());
        }
    }

    @FunctionalInterface
    private interface LineApplier {
        void apply(String[] row, DuplicateSkuMode mode);
    }

    private int applyOkLines(ImportPreviewResponse preview, List<String[]> rows,
                             LineApplier applier, DuplicateSkuMode duplicateMode) {
        int success = 0;
        for (ImportLineResult line : preview.getLines()) {
            if ("OK".equals(line.getStatus()) && line.getAction() != null && !"SKIP".equals(line.getAction())) {
                applier.apply(rows.get(line.getDataRowIndex()), duplicateMode);
                success++;
            }
        }
        return success;
    }

    private ImportLineResult duplicateResult(int line, int dataRowIndex, String id,
                                             DuplicateSkuMode mode, String rejectMessage) {
        if (mode == DuplicateSkuMode.REJECT) {
            return error(line, dataRowIndex, id, rejectMessage);
        }
        if (mode == DuplicateSkuMode.SKIP) {
            return ok(line, dataRowIndex, id, "SKIP");
        }
        return ok(line, dataRowIndex, id, "UPDATE");
    }

    private ImportPreviewResponse buildPackagingPreview(List<String[]> rows, boolean forValidate) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), PACKAGING_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String productSku = TabularFileHelper.cell(row, 0);
            String nom = TabularFileHelper.cell(row, 1);
            String quantiteBase = TabularFileHelper.cell(row, 3);
            if (productSku.isBlank() && nom.isBlank()) {
                continue;
            }
            if (productSku.isBlank()) {
                lines.add(error(lineNum, i, nom, "productSku obligatoire"));
                continue;
            }
            if (nom.isBlank()) {
                lines.add(error(lineNum, i, productSku, "Nom du conditionnement obligatoire"));
                continue;
            }
            Product product = productRepository.findBySku(productSku).orElse(null);
            if (product == null) {
                lines.add(error(lineNum, i, productSku, "Produit inconnu"));
                continue;
            }
            if (quantiteBase.isBlank() || parseDecimal(quantiteBase) == null
                    || parseDecimal(quantiteBase).compareTo(BigDecimal.ZERO) <= 0) {
                lines.add(error(lineNum, i, nom, "quantiteBase invalide"));
                continue;
            }
            if (packagingRepository.findFirstByProductIdAndNomIgnoreCase(product.getId(), nom).isPresent()) {
                lines.add(error(lineNum, i, nom, "Conditionnement deja existant pour ce produit"));
                continue;
            }
            lines.add(ok(lineNum, i, productSku + "/" + nom, "CREATE"));
        }
        return summarize(lines);
    }

    private void applyPackagingLine(String[] row) {
        Product product = productRepository.findBySku(TabularFileHelper.cell(row, 0))
                .orElseThrow(() -> new BusinessException("Produit inconnu"));
        String nom = TabularFileHelper.cell(row, 1);
        boolean defaultAchat = "true".equalsIgnoreCase(TabularFileHelper.cell(row, 5))
                || "1".equals(TabularFileHelper.cell(row, 5))
                || "oui".equalsIgnoreCase(TabularFileHelper.cell(row, 5));
        String codeBarre = emptyToNull(TabularFileHelper.cell(row, 4));
        if (codeBarre == null) {
            codeBarre = barcodeService.allocateEan13(barcodeRegistryService::isTaken);
        }
        packagingRepository.save(ProductPackaging.builder()
                .product(product)
                .nom(nom)
                .symbole(emptyToNull(TabularFileHelper.cell(row, 2)))
                .quantiteBase(parseDecimal(TabularFileHelper.cell(row, 3)))
                .codeBarre(codeBarre)
                .prixVente(PackagingService.resolvePrixVente(
                        product,
                        parseDecimal(TabularFileHelper.cell(row, 3)),
                        parseDecimal(emptyToNull(TabularFileHelper.cell(row, 6)))))
                .defaultAchat(defaultAchat)
                .defaultVente(false)
                .usableForSale(true)
                .usableForPurchase(true)
                .principal(defaultAchat)
                .actif(true)
                .build());
    }

    private ImportPreviewResponse buildInitialStockPreview(List<String[]> rows, boolean forValidate) {
        if (rows.isEmpty()) {
            throw new BusinessException("Fichier vide");
        }
        validateHeaders(rows.get(0), INITIAL_STOCK_HEADERS);
        List<ImportLineResult> lines = new ArrayList<>();
        int lineNum = 0;
        for (int i = 1; i < rows.size(); i++) {
            lineNum++;
            String[] row = rows.get(i);
            String productSku = TabularFileHelper.cell(row, 0);
            String warehouseCode = TabularFileHelper.cell(row, 2);
            String locationCode = TabularFileHelper.cell(row, 3);
            String quantity = TabularFileHelper.cell(row, 4);
            if (productSku.isBlank() && quantity.isBlank()) {
                continue;
            }
            if (productSku.isBlank()) {
                lines.add(error(lineNum, i, "", "productSku obligatoire"));
                continue;
            }
            if (warehouseCode.isBlank() || locationCode.isBlank()) {
                lines.add(error(lineNum, i, productSku, "warehouseCode et locationCode obligatoires"));
                continue;
            }
            Product product = productRepository.findBySku(productSku).orElse(null);
            if (product == null) {
                lines.add(error(lineNum, i, productSku, "Produit inconnu"));
                continue;
            }
            Warehouse warehouse = warehouseRepository.findByCode(warehouseCode).orElse(null);
            if (warehouse == null) {
                lines.add(error(lineNum, i, productSku, "Entrepot inconnu: " + warehouseCode));
                continue;
            }
            if (locationRepository.findByWarehouseIdAndCode(warehouse.getId(), locationCode).isEmpty()) {
                lines.add(error(lineNum, i, productSku, "Emplacement inconnu: " + locationCode));
                continue;
            }
            BigDecimal qty = parseDecimal(quantity);
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                lines.add(error(lineNum, i, productSku, "Quantite invalide"));
                continue;
            }
            String variantSku = TabularFileHelper.cell(row, 1);
            if (!variantSku.isBlank()) {
                ProductVariant variant = variantRepository.findBySku(variantSku).orElse(null);
                if (variant == null || !variant.getProduct().getId().equals(product.getId())) {
                    lines.add(error(lineNum, i, productSku, "Variante inconnue: " + variantSku));
                    continue;
                }
            }
            lines.add(ok(lineNum, i, productSku, "CREATE"));
        }
        return summarize(lines);
    }

    private void applyInitialStockLine(String[] row, String actor, String fileName) {
        Product product = productRepository.findBySku(TabularFileHelper.cell(row, 0)).orElseThrow();
        Warehouse warehouse = warehouseRepository.findByCode(TabularFileHelper.cell(row, 2)).orElseThrow();
        Location location = locationRepository.findByWarehouseIdAndCode(
                warehouse.getId(), TabularFileHelper.cell(row, 3)).orElseThrow();
        Long variantId = null;
        String variantSku = TabularFileHelper.cell(row, 1);
        if (!variantSku.isBlank()) {
            variantId = variantRepository.findBySku(variantSku).map(ProductVariant::getId).orElseThrow();
        } else {
            List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
            if (variants.size() == 1) {
                variantId = variants.get(0).getId();
            }
        }
        Long lotId = resolveLotId(product, variantId, row);

        StockOperationRequest request = new StockOperationRequest();
        request.setProductId(product.getId());
        request.setVariantId(variantId);
        request.setWarehouseId(warehouse.getId());
        request.setLocationId(location.getId());
        request.setLotId(lotId);
        request.setQuantityBase(parseDecimal(TabularFileHelper.cell(row, 4)));
        request.setUtilisateur(actor);
        stockService.applyInitialStock(request, fileName);
    }

    private Long resolveLotId(Product product, Long variantId, String[] row) {
        String lotNumber = TabularFileHelper.cell(row, 5);
        if (lotNumber.isBlank()) {
            return null;
        }
        ProductVariant variant = variantId != null
                ? variantRepository.findById(variantId).orElse(null)
                : null;
        String expiryStr = TabularFileHelper.cell(row, 6);
        LocalDate expiry = expiryStr.isBlank() ? null : LocalDate.parse(expiryStr);

        var existing = variant != null
                ? lotRepository.findByProductIdAndVariantIdAndNumeroLot(product.getId(), variant.getId(), lotNumber)
                : lotRepository.findByProductIdAndVariantIsNullAndNumeroLot(product.getId(), lotNumber);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        Lot lot = lotRepository.save(Lot.builder()
                .product(product)
                .variant(variant)
                .numeroLot(lotNumber)
                .datePeremption(expiry)
                .build());
        return lot.getId();
    }

    private ImportJob saveJob(ImportType type, String fileName, String actor,
                              int total, int success, int errors, List<ImportLineResult> lines) {
        ImportJobStatus status = errors == 0
                ? ImportJobStatus.COMPLETED
                : (success > 0 ? ImportJobStatus.PARTIAL : ImportJobStatus.FAILED);
        ImportJob job = ImportJob.builder()
                .importType(type)
                .status(status)
                .fileName(fileName != null ? fileName : "import")
                .createdBy(actor)
                .totalRows(total)
                .successRows(success)
                .errorRows(errors)
                .errorReport(serializeErrors(lines))
                .completedAt(Instant.now())
                .build();
        return importJobRepository.save(job);
    }

    private String serializeErrors(List<ImportLineResult> lines) {
        List<ImportLineResult> errors = lines.stream()
                .filter(l -> "ERROR".equals(l.getStatus()))
                .toList();
        if (errors.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            return errors.toString();
        }
    }

    private ImportJobResponse toJobResponse(ImportJob job) {
        return ImportJobResponse.builder()
                .id(job.getId())
                .importType(job.getImportType().name())
                .status(job.getStatus().name())
                .fileName(job.getFileName())
                .createdBy(job.getCreatedBy())
                .totalRows(job.getTotalRows())
                .successRows(job.getSuccessRows())
                .errorRows(job.getErrorRows())
                .errorReport(job.getErrorReport())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private ImportPreviewResponse summarize(List<ImportLineResult> lines) {
        int errors = (int) lines.stream().filter(l -> "ERROR".equals(l.getStatus())).count();
        return ImportPreviewResponse.builder()
                .totalRows(lines.size())
                .validRows(lines.size() - errors)
                .errorRows(errors)
                .lines(lines)
                .build();
    }

    private ImportLineResult ok(int line, int dataRowIndex, String id, String action) {
        return ImportLineResult.builder()
                .lineNumber(line)
                .dataRowIndex(dataRowIndex)
                .status("OK")
                .action(action)
                .identifier(id)
                .build();
    }

    private ImportLineResult error(int line, int dataRowIndex, String id, String message) {
        return ImportLineResult.builder()
                .lineNumber(line)
                .dataRowIndex(dataRowIndex)
                .status("ERROR")
                .identifier(id)
                .message(message)
                .build();
    }

    private List<String[]> readData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Fichier obligatoire");
        }
        return TabularFileHelper.read(file);
    }

    private void validateHeaders(String[] headerRow, List<String> expected) {
        for (int i = 0; i < expected.size(); i++) {
            String actual = TabularFileHelper.cell(headerRow, i);
            if (!expected.get(i).equalsIgnoreCase(actual)) {
                throw new BusinessException("En-tete invalide colonne " + (i + 1)
                        + " : attendu '" + expected.get(i) + "', trouve '" + actual + "'");
            }
        }
    }

    private Category resolveCategory(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        return categoryRepository.findFirstByNomIgnoreCase(nom.trim()).orElse(null);
    }

    private Brand resolveBrand(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        return brandRepository.findFirstByNomIgnoreCase(nom.trim()).orElse(null);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
