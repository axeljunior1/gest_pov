package com.erp.products.controller;

import com.erp.products.dto.LoginRequest;
import com.erp.products.dto.LoginResponse;
import com.erp.products.dto.UserResponse;
import com.erp.products.security.CurrentUserService;
import com.erp.products.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        String email = currentUserService.getCurrentUserEmail();
        if (email == null) {
            throw new com.erp.products.exception.BusinessException("Non authentifie");
        }
        return authService.me(email);
    }
}
