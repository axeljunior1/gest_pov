package com.erp.products.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private static final String SYSTEM = "system";

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
}
