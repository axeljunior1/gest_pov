package com.erp.products.repository;

import com.erp.products.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);

    @Query("SELECT c FROM Category c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Category> searchByNom(String nom);

    Optional<Category> findFirstByNomIgnoreCase(String nom);
}
