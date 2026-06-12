package com.erp.products.service;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.User;
import com.erp.products.dto.LoginRequest;
import com.erp.products.dto.LoginResponse;
import com.erp.products.dto.UserResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.mapper.UserMapper;
import com.erp.products.repository.UserRepository;
import com.erp.products.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException e) {
            throw new BusinessException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new BusinessException("Utilisateur non trouve"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserResponse userResponse = userMapper.toUserResponse(user);
        List<String> permissions = userResponse.getPermissions();

        String token = jwtService.generateToken(email, permissions);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userResponse)
                .permissions(permissions)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email.toLowerCase())
                .orElseThrow(() -> new BusinessException("Utilisateur non trouve"));
        return userMapper.toUserResponse(user);
    }
}
