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

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final ProductPackagingRepository packagingRepository;
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
                if (duplicateMode == DuplicateSkuMode.REJECT) {
                    lines.add(error(lineNum, i, sku, "SKU deja existant"));
                } else {
                    lines.add(ok(lineNum, i, sku, "UPDATE"));
                }
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
            product.setMarque(emptyToNull(TabularFileHelper.cell(row, 3)));
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
                    .marque(emptyToNull(TabularFileHelper.cell(row, 3)))
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
        packagingRepository.save(ProductPackaging.builder()
                .product(product)
                .nom(nom)
                .symbole(emptyToNull(TabularFileHelper.cell(row, 2)))
                .quantiteBase(parseDecimal(TabularFileHelper.cell(row, 3)))
                .codeBarre(emptyToNull(TabularFileHelper.cell(row, 4)))
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
