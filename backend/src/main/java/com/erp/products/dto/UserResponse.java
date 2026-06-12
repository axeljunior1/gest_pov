package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Instant createdAt;
    private List<String> roles;
    private List<String> permissions;
}
