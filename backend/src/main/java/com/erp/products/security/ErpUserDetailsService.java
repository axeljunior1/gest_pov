package com.erp.products.security;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ErpUserDetailsService implements UserDetailsService {

    private final com.erp.products.repository.UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve: " + email));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Compte desactive");
        }

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        user.getRoles().stream()
                .map(Role::getCode)
                .map(code -> "ROLE_" + code)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
