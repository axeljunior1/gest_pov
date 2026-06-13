package com.erp.products.dto;

import lombok.Data;

@Data
public class CustomerQuickCreateRequest {
    private String lastName;
    private String firstName;
    private String phone;
}
