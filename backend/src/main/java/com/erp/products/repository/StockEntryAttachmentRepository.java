package com.erp.products.repository;

import com.erp.products.domain.entity.StockEntryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryAttachmentRepository extends JpaRepository<StockEntryAttachment, Long> {
}
