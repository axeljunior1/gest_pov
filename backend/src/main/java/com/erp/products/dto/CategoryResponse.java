package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String nom;
    private Long parentId;
    private String parentNom;
    private List<CategoryResponse> children;
    private Instant createdAt;
    private Instant updatedAt;
}
