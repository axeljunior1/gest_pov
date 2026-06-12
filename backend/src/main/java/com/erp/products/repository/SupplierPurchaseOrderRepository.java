package com.erp.products.repository;

import com.erp.products.domain.entity.SupplierPurchaseOrder;
import com.erp.products.domain.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SupplierPurchaseOrderRepository extends JpaRepository<SupplierPurchaseOrder, Long> {

    List<SupplierPurchaseOrder> findByStatusAndExpectedDeliveryDateBefore(
            PurchaseOrderStatus status, LocalDate date);
}
