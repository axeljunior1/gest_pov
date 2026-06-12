package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean isSystem;
    private List<String> permissions;
}
