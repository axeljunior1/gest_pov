package com.erp.products.repository;

import com.erp.products.domain.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findByKey(String key);

    List<AppSetting> findByIsPublicTrueOrderByKeyAsc();
}
