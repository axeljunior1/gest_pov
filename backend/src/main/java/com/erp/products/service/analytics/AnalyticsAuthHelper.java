package com.erp.products.service.analytics;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class AnalyticsAuthHelper {

    private AnalyticsAuthHelper() {
    }

    public static boolean has(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(permission)
                        || auth.equals("ROLE_SUPER_ADMIN")
                        || matchesWildcard(auth, permission));
    }

    public static boolean hasAny(Authentication authentication, String... permissions) {
        if (permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (has(authentication, permission)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesWildcard(String authority, String permission) {
        if (!authority.endsWith(".*")) {
            return false;
        }
        String prefix = authority.substring(0, authority.length() - 1);
        return permission.startsWith(prefix);
    }
}
