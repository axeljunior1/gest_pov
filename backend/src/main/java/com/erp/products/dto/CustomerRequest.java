package com.erp.products.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerRequest {
    private String firstName;
    private String lastName;
    private String companyName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String address;
    private String city;
    private String notes;
    private Boolean isActive;
}
