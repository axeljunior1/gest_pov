package com.erp.products.repository;

import com.erp.products.domain.entity.ProductDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long> {

    List<ProductDocument> findByProductId(Long productId);
}
