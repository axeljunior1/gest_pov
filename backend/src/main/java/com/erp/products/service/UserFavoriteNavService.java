package com.erp.products.service;

import com.erp.products.domain.entity.User;
import com.erp.products.domain.entity.UserFavoriteNavItem;
import com.erp.products.repository.UserFavoriteNavItemRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFavoriteNavService {

    private final UserFavoriteNavItemRepository repository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<String> list() {
        User user = currentUserService.requireCurrentUser();
        return repository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(UserFavoriteNavItem::getNavItemKey)
                .toList();
    }

    @Transactional
    public void add(String navItemKey) {
        User user = currentUserService.requireCurrentUser();
        if (repository.existsByUserIdAndNavItemKey(user.getId(), navItemKey)) {
            return;
        }
        repository.save(UserFavoriteNavItem.builder()
                .user(user)
                .navItemKey(navItemKey)
                .build());
    }

    @Transactional
    public void remove(String navItemKey) {
        User user = currentUserService.requireCurrentUser();
        repository.findByUserIdAndNavItemKey(user.getId(), navItemKey)
                .ifPresent(repository::delete);
    }
}
