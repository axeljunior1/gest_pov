package com.gestpov.desktop.net;

import com.gestpov.desktop.model.UserAccount;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserClient {

    private final ApiClient api;

    public UserClient(ApiClient api) {
        this.api = api;
    }

    public List<UserAccount> list() throws ApiException {
        return JsonLists.mapArray(api.get("/api/users"), UserAccount::fromJson);
    }

    public UserAccount getById(long id) throws ApiException {
        return UserAccount.fromJson(api.get("/api/users/" + id));
    }

    public UserAccount create(String firstName, String lastName, String email, String password, String badgeCode,
                              String pin, boolean active, List<Long> roleIds) throws ApiException {
        return UserAccount.fromJson(api.post("/api/users",
                body(firstName, lastName, email, password, badgeCode, pin, active, roleIds)));
    }

    public UserAccount update(long id, String firstName, String lastName, String email, String password,
                              String badgeCode, String pin, boolean active, List<Long> roleIds) throws ApiException {
        return UserAccount.fromJson(api.put("/api/users/" + id,
                body(firstName, lastName, email, password, badgeCode, pin, active, roleIds)));
    }

    public void delete(long id) throws ApiException {
        api.delete("/api/users/" + id);
    }

    private static Map<String, Object> body(String firstName, String lastName, String email, String password,
                                            String badgeCode, String pin, boolean active, List<Long> roleIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        if (password != null && !password.isBlank()) {
            body.put("password", password);
        }
        body.put("badgeCode", badgeCode == null ? "" : badgeCode.trim());
        if (pin != null && !pin.isBlank()) {
            body.put("pin", pin);
        }
        body.put("isActive", active);
        body.put("roleIds", roleIds == null ? List.of() : roleIds);
        return body;
    }
}
