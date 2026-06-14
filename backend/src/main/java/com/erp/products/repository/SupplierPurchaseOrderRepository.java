package com.erp.products.repository;

import com.erp.products.domain.entity.SupplierPurchaseOrder;
import com.erp.products.domain.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierPurchaseOrderRepository extends JpaRepository<SupplierPurchaseOrder, Long> {

    Optional<SupplierPurchaseOrder> findByReference(String reference);

    List<SupplierPurchaseOrder> findByStatusAndExpectedDeliveryDateBefore(
            PurchaseOrderStatus status, LocalDate date);

    @Query("""
            SELECT o FROM SupplierPurchaseOrder o
            LEFT JOIN FETCH o.supplier
            LEFT JOIN FETCH o.lines l
            LEFT JOIN FETCH l.product
            WHERE (:status IS NULL OR o.status = :status)
              AND (:supplierId IS NULL OR o.supplier.id = :supplierId)
            ORDER BY o.createdAt DESC
            """)
    List<SupplierPurchaseOrder> findFiltered(PurchaseOrderStatus status, Long supplierId);

    @Query("""
            SELECT o FROM SupplierPurchaseOrder o
            LEFT JOIN FETCH o.supplier
            LEFT JOIN FETCH o.warehouse
            LEFT JOIN FETCH o.lines l
            LEFT JOIN FETCH l.product
            LEFT JOIN FETCH l.variant
            WHERE o.id = :id
            """)
    Optional<SupplierPurchaseOrder> findDetailedById(Long id);
}
