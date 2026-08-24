package com.erp.products.repository;

import com.erp.products.domain.entity.UserFavoriteNavItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteNavItemRepository extends JpaRepository<UserFavoriteNavItem, Long> {

    List<UserFavoriteNavItem> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<UserFavoriteNavItem> findByUserIdAndNavItemKey(Long userId, String navItemKey);

    boolean existsByUserIdAndNavItemKey(Long userId, String navItemKey);
}
