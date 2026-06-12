package com.erp.products.service;

import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.dto.UserRequest;
import com.erp.products.dto.UserResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.UserMapper;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(u -> userRepository.findByIdWithRolesAndPermissions(u.getId()).orElse(u))
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email deja utilise: " + email);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Mot de passe obligatoire a la creation");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .roles(loadRoles(request.getRoleIds()))
                .build();

        User saved = userRepository.save(user);
        auditService.log("User", saved.getId(), com.erp.products.domain.enums.AuditAction.CREATION,
                "Utilisateur cree: " + saved.getEmail(), currentUserService.getCurrentUserEmailOrDefault());
        return userMapper.toUserResponse(userRepository.findByIdWithRolesAndPermissions(saved.getId()).orElse(saved));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));

        String email = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email deja utilise: " + email);
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        user.setRoles(loadRoles(request.getRoleIds()));

        User saved = userRepository.save(user);
        auditService.log("User", saved.getId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Utilisateur modifie: " + saved.getEmail(), currentUserService.getCurrentUserEmailOrDefault());
        return userMapper.toUserResponse(userRepository.findByIdWithRolesAndPermissions(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));
        if ("admin@erp.local".equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("Impossible de supprimer le compte administrateur systeme");
        }
        userRepository.delete(user);
        auditService.log("User", id, com.erp.products.domain.enums.AuditAction.SUPPRESSION,
                "Utilisateur supprime: " + user.getEmail(), currentUserService.getCurrentUserEmailOrDefault());
    }

    private Set<Role> loadRoles(List<Long> roleIds) {
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            roles.add(roleRepository.findByIdWithPermissions(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role non trouve: " + roleId)));
        }
        return roles;
    }
}
