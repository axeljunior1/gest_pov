package com.erp.products.repository;

import com.erp.products.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByModuleOrderByCode(String module);

    List<Permission> findAllByOrderByModuleAscCodeAsc();
}
