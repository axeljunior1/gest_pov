package com.erp.products.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("permissionChecker")
@Profile("!test")
public class PermissionChecker {

    public boolean has(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(permission)
                        || auth.equals("ROLE_SUPER_ADMIN")
                        || auth.equals("products.*")
                        || matchesWildcard(auth, permission));
    }

    private boolean matchesWildcard(String authority, String permission) {
        if (!authority.endsWith(".*")) {
            return false;
        }
        String prefix = authority.substring(0, authority.length() - 1);
        return permission.startsWith(prefix);
    }
}
