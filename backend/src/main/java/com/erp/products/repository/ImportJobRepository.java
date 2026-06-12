package com.erp.products.repository;

import com.erp.products.domain.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findTop50ByOrderByCreatedAtDesc();
}
