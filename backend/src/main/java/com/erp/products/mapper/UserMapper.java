package com.erp.products.mapper;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.dto.PermissionResponse;
import com.erp.products.dto.RoleResponse;
import com.erp.products.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();

        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    public RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .permissions(role.getPermissions().stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList())
                .build();
    }

    public PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .module(p.getModule())
                .build();
    }
}
