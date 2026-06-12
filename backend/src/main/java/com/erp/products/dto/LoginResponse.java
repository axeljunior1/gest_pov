package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
    private List<String> permissions;
}
