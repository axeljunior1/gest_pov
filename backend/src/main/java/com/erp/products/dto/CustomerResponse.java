package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private String customerNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String companyName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String address;
    private String city;
    private String notes;
    private Integer loyaltyPoints;
    private String loyaltyTier;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
