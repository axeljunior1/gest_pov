package com.erp.products.security;

import com.erp.products.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ErpUserDetailsService implements UserDetailsService {

    private final com.erp.products.repository.UserRepository userRepository;
    private final UserAuthorityService userAuthorityService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve: " + email));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Compte desactive");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(userAuthorityService.buildAuthorities(user))
                .build();
    }
}
