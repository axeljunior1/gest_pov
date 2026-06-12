package com.erp.products.service;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.dto.PermissionResponse;
import com.erp.products.dto.RoleResponse;
import com.erp.products.exception.BusinessException;
import com.erp.products.exception.ResourceNotFoundException;
import com.erp.products.mapper.UserMapper;
import com.erp.products.repository.PermissionRepository;
import com.erp.products.repository.RoleRepository;
import com.erp.products.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAllWithPermissions().stream()
                .map(userMapper::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role non trouve: " + id));
        return userMapper.toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(userMapper::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updatePermissions(Long roleId, List<String> permissionCodes) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role non trouve: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystem()) && "SUPER_ADMIN".equals(role.getCode())) {
            throw new BusinessException("Les permissions SUPER_ADMIN ne peuvent pas etre modifiees");
        }

        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissions.add(permissionRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvee: " + code)));
        }
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        auditService.log("Role", saved.getId(), com.erp.products.domain.enums.AuditAction.MODIFICATION,
                "Permissions role " + saved.getCode() + " mises a jour",
                currentUserService.getCurrentUserEmailOrDefault());
        return userMapper.toRoleResponse(roleRepository.findByIdWithPermissions(saved.getId()).orElse(saved));
    }
}
