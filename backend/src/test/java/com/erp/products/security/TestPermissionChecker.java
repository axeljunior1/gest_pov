package com.erp.products.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionChecker")
@Profile("test")
public class TestPermissionChecker {

    public boolean has(Authentication authentication, String permission) {
        return true;
    }
}
