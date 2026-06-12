package com.erp.products.controller;

import com.erp.products.dto.PermissionResponse;
import com.erp.products.dto.RoleResponse;
import com.erp.products.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'roles.read')")
    public List<RoleResponse> list() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'roles.read')")
    public RoleResponse getById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @GetMapping("/permissions")
    @PreAuthorize("@permissionChecker.has(authentication, 'roles.read')")
    public List<PermissionResponse> listPermissions() {
        return roleService.findAllPermissions();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@permissionChecker.has(authentication, 'roles.update')")
    public RoleResponse updatePermissions(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body) {
        List<String> codes = body.getOrDefault("permissions", List.of());
        return roleService.updatePermissions(id, codes);
    }
}
