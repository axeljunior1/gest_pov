package com.erp.products.repository;

import com.erp.products.domain.entity.AlertRule;
import com.erp.products.domain.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    Optional<AlertRule> findByAlertType(AlertType alertType);
}
