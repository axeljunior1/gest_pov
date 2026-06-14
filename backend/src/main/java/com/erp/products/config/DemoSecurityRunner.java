package com.erp.products.config;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Execute une action POS / metier avec le contexte securite d'un utilisateur demo. */
public final class DemoSecurityRunner {

    private DemoSecurityRunner() {
    }

    public static void runAs(User user, Runnable action) {
        runAs(user, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T runAs(User user, Supplier<T> action) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPasswordHash(),
                authorities(user));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private static Set<GrantedAuthority> authorities(User user) {
        Set<GrantedAuthority> auths = new HashSet<>();
        if (user.getRoles() == null) {
            return auths;
        }
        for (Role role : user.getRoles()) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            if (role.getPermissions() != null) {
                for (Permission permission : role.getPermissions()) {
                    auths.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }
        }
        return auths;
    }
}
