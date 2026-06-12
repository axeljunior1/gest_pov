package com.erp.products.repository;

import com.erp.products.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(Long id);
}
