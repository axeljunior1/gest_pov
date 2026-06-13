package com.erp.products.security;

import com.erp.products.domain.entity.User;
import com.erp.products.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String SYSTEM = "system";

    private final UserRepository userRepository;

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return email;
        }
        return auth.getName();
    }

    public String getCurrentUserEmailOrDefault() {
        String email = getCurrentUserEmail();
        return email != null && !email.isBlank() ? email : SYSTEM;
    }

    public boolean isAuthenticated() {
        return getCurrentUserEmail() != null;
    }

    /** Priorise l'utilisateur JWT ; le fallback client n'est accepte qu'en profil test / non authentifie. */
    public String resolveActor(String clientProvided) {
        if (isAuthenticated()) {
            return getCurrentUserEmailOrDefault();
        }
        return clientProvided != null && !clientProvided.isBlank() ? clientProvided : SYSTEM;
    }

    public Optional<User> getCurrentUser() {
        String email = getCurrentUserEmail();
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email);
    }

    public User requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new com.erp.products.exception.BusinessException("Utilisateur non authentifie"));
    }
}
