package com.erp.products.security;

import org.springframework.security.core.Authentication;

public interface PermissionEvaluator {

    boolean has(Authentication authentication, String permission);
}
