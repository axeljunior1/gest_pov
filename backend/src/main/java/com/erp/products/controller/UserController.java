package com.erp.products.controller;

import com.erp.products.dto.UserRequest;
import com.erp.products.dto.UserResponse;
import com.erp.products.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'users.read')")
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'users.read')")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has(authentication, 'users.create')")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has(authentication, 'users.update')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionChecker.has(authentication, 'users.delete')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
