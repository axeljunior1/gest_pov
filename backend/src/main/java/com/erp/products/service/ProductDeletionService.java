package com.erp.products.service;

import com.erp.products.domain.entity.Product;
import com.erp.products.domain.enums.AuditAction;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.repository.AuditLogRepository;
import com.erp.products.repository.PriceHistoryRepository;
import com.erp.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDeletionService {

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    @Transactional
    public void delete(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé: " + productId));

        assertDeletable(productId);
        deleteOperationalData(productId);

        priceHistoryRepository.deleteByProductId(productId);
        auditLogRepository.deleteByEntityTypeAndEntityId("Product", productId);

        product.getImages().forEach(img -> fileStorageService.delete(img.getFilePath()));
        product.getDocuments().forEach(doc -> fileStorageService.delete(doc.getFilePath()));

        productRepository.delete(product);
        auditService.log("Product", productId, AuditAction.SUPPRESSION, "Produit supprimé: " + product.getNom());
    }

    private void assertDeletable(Long productId) {
        long saleLines = count("SELECT COUNT(*) FROM sale_lines WHERE product_id = ?", productId);
        if (saleLines > 0) {
            throw new BusinessException(
                    "Suppression impossible : ce produit est utilisé dans des ventes. Désactivez-le ou archivez-le.");
        }

        long refundLines = count("SELECT COUNT(*) FROM sale_refund_lines WHERE product_id = ?", productId);
        if (refundLines > 0) {
            throw new BusinessException(
                    "Suppression impossible : ce produit est lié à des retours. Désactivez-le ou archivez-le.");
        }

        long purchaseOrderLines = count(
                "SELECT COUNT(*) FROM supplier_purchase_order_lines WHERE product_id = ?", productId);
        if (purchaseOrderLines > 0) {
            throw new BusinessException(
                    "Suppression impossible : ce produit figure sur des commandes fournisseur.");
        }
    }

    private void deleteOperationalData(Long productId) {
        jdbcTemplate.update("DELETE FROM stock_valuation_movements WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_valuation WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_movements WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_reservations WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_transfer_lines WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_exit_lines WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_entry_lines WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM inventory_count_lines WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM stock_items WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM lots WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM alerts WHERE product_id = ?", productId);
        jdbcTemplate.update("DELETE FROM alert_settings WHERE product_id = ?", productId);
        jdbcTemplate.update("""
                DELETE FROM product_variant_attribute_values
                WHERE product_variant_id IN (SELECT id FROM product_variants WHERE product_id = ?)
                """, productId);
    }

    private long count(String sql, Long productId) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, productId);
        return result != null ? result : 0L;
    }
}
