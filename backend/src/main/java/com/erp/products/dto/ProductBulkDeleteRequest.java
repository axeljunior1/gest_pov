package com.erp.products.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ProductBulkDeleteRequest {

    @NotEmpty(message = "Au moins un identifiant produit est requis")
    private List<Long> ids;
}
