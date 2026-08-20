package com.gestpov.desktop.net;

import com.gestpov.desktop.model.Permission;
import com.gestpov.desktop.model.Role;

import java.util.List;
import java.util.Map;

public class RoleClient {

    private final ApiClient api;

    public RoleClient(ApiClient api) {
        this.api = api;
    }

    public List<Role> list() throws ApiException {
        return JsonLists.mapArray(api.get("/api/roles"), Role::fromJson);
    }

    public Role getById(long id) throws ApiException {
        return Role.fromJson(api.get("/api/roles/" + id));
    }

    public List<Permission> listPermissions() throws ApiException {
        return JsonLists.mapArray(api.get("/api/roles/permissions"), Permission::fromJson);
    }

    public Role updatePermissions(long id, List<String> permissionCodes) throws ApiException {
        return Role.fromJson(api.put("/api/roles/" + id + "/permissions",
                Map.of("permissions", permissionCodes == null ? List.of() : permissionCodes)));
    }
}
